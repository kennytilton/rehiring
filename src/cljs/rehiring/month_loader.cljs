(ns rehiring.month-loader
  (:require
    [cljs.pprint :as pp]

    [clojure.walk :as walk]
    [re-frame.core
     :refer [subscribe reg-sub dispatch reg-event-db reg-event-fx]
     :as rfr]
    [rehiring.utility :as utl]
    [rehiring.job-parse :as parse]))

(defn gMonthlies-cljs []
  ;; gMonthlies defined in index.html for extensibility
  ;; translated here to CLJS-ese, except...
  ;; hnId remains camel-case :hnId

  (walk/keywordize-keys (js->clj js/gMonthlies)))

(defn get-monthly-def
  "Retrieve month info based on HN message Id"
  [hn-id]
  (some #(when (= (:hnId %) hn-id) %)
    (gMonthlies-cljs)))

(defn month-page-urls
  "Generate the URLs to scrape given mont info
  hard-coded in index.html"
  [month-hn-id]
  (when month-hn-id
    (if-let [mo-def (get-monthly-def month-hn-id)]          ;; hard-coded table in index.html
      (map (fn [pg-offset]
             ;; files are numbered off-by-one to match the page param on HN
             (pp/cl-format nil "files/~a/~a.html" month-hn-id (inc pg-offset)))
        (range (:pgCount mo-def)))
      (throw (js/Exception. (str "msg id " month-hn-id " not defined in gMonthlies table."))))))

(defn unprocessed-month [month-hn-id]
  (let [urls (month-page-urls month-hn-id)]
    {:month-hn-id          month-hn-id
     :phase                :cull-athings                    ;; ...or :parse-jobs
     :month-load-complete? false
     :page-url-count       (count urls)
     :page-urls-remaining  urls
     :jobs-seen            #{}
     :athings              []
     :jobs                 []}))

(rfr/reg-sub :month-phase
  (fn [db]
    (get-in db [:month-load-task :phase])))

(rfr/reg-sub :month-jobs
  (fn [db]
    (get-in db [:month-load-task :jobs])))

(rfr/reg-sub :month-progress-max
  (fn [[_] _]
    [(rfr/subscribe [:month-load-task])])
  (fn [[task]]
    (if (= :cull-athings (:phase task))
      (:page-url-count task)
      (count (:athings task)))))

(rfr/reg-sub :month-progress-made
  (fn [[_] _]
    [(rfr/subscribe [:month-load-task])])
  (fn [[task]]
    (if (= :cull-athings (:phase task))
      (- (:page-url-count task) (count (:page-urls-remaining task)))
      (count (:jobs task)))))

;;; --- kick off month load from initilaize-db or when user selects new month (see pick-a-month) -----

(rfr/reg-event-db :month-set
  (fn [db [_ month-hn-id]]
    (let [mo-def (get-monthly-def month-hn-id)]
      (assoc db :month-load-task (unprocessed-month month-hn-id)))))

;;; --- a component that loads pages into iframes and kicks off their processing ---------------------

(declare mk-page-loader job-page-athings)

(rfr/reg-sub :month-load-task
  (fn [db]
    (:month-load-task db)))

(defn job-listing-loader
  "The main player in loading a month. It spawns one child loader whenever there is a URL to be scraped."
  []
  (fn []
    [:div {:style {:display "none"}}
     (let [task @(rfr/subscribe [:month-load-task])]
       (when (seq (:page-urls-remaining task))
         (println :mk-pg-loader-for (:page-urls-remaining task))
         [mk-page-loader task]))]))


;;; --- dev-time limits -----------------------------
;;; n.b.: these will be limits *per page*

(def ATHING-PARSE-MAX 50)
(def JOB-LOAD-MAX 10000)                                    ;; todo: still needed?

(defn job-page-athings [ifr-dom]
  (when-let [cont-doc (.-contentDocument ifr-dom)]
    (let [hn-body (aget (.getElementsByTagName cont-doc "body") 0)]
      (let [a-things (take ATHING-PARSE-MAX (prim-seq (.querySelectorAll hn-body ".athing")))]
        (set! (.-innerHTML hn-body) "")                     ;; free up memory
        a-things))))

(defn mk-page-loader []
  (fn [task]
    (assert (first (:page-urls-remaining task)))
    [:iframe {:src     (first (:page-urls-remaining task))
              :on-load #(dispatch
                          ^:flush-dom [:page-athings-culled
                                       (utl/update-multi task
                                         [:page-urls-remaining rest]
                                         [:athings concat (job-page-athings (.-target %))])])}]))

(reg-event-fx :page-athings-culled
  (fn [{:keys [db]} [_ task]]
    (if (empty? (:page-urls-remaining task))

      ;; Here we make the phase transition from culling raw nodes
      ;; from HN pages to parsing those nodes identified as jobs
      {:db       (assoc db :month-load-task
                           (assoc task :phase :parse-jobs))
       :dispatch ^:flush-dom [:cull-jobs-from-athings]}

      ;; Next triggers page-loader component to load next page so no
      ;; need to dispatch an event to keeo things movung -- the
      ;; page loader will kink off this event itself.
      {:db (assoc db :month-load-task task)})))

(def ATHING_CHUNK_SZ 20)                                    ;; bigger chunks zoom due to 15ms clock; use small value to see progress bar working

(reg-event-fx :cull-jobs-from-athings
  (fn [{:keys [db]} [_]]
    (let [task (:month-load-task db)
          chunk (take ATHING_CHUNK_SZ (get-in db [:month-load-task :athings]))]

      (if (seq chunk)
        (let [new-jobs (filter #(:OK %) (map #(parse/job-parse % (:jobs-seen task)) chunk))]

          {:db       (assoc db :month-load-task
                               (utl/update-multi task
                                 [:athings nthrest ATHING_CHUNK_SZ]
                                 [:jobs concat new-jobs]
                                 [:jobs-seen clojure.set/union (into #{} (map :hn-id new-jobs))]))
           ;;
           ;; either of these next two work. I prefer not guessing at a delay.
           ;; but zero works, so under the hood these may be the same
           ;;
           ;; :dispatch-later [{:ms 0 :dispatch [:cull-jobs-from-athings]}]
           :dispatch ^:flush-dom [:cull-jobs-from-athings]
           })
        (do (println :athings-exhausted)
            {:db (assoc-in db [:month-load-task :month-load-complete?] true)})))))

;;; --- sub jobs-filtered watches this ---------------------------------------

(reg-sub :month-load-complete?
  (fn [db]
    (get-in db [:month-load-task :month-load-complete?])))
