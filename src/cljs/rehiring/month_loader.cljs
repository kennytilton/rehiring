(ns rehiring.month-loader
  (:require
    [cljs.pprint :as pp]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [re-frame.core :as rfr]
    [rehiring.utility :as utl]
    [rehiring.job-parse :as parse]))

(defn gMonthlies-cljs []
  ;; gMonthlies defined in index.html for extensibility
  (walk/keywordize-keys (js->clj js/gMonthlies)))

(defn get-monthly-def [hn-id]
  (some #(when (= (:hnId %) hn-id) %)
    (gMonthlies-cljs)))

(defn month-page-urls [month-hn-id]
  (when month-hn-id
    (if-let [mo-def (get-monthly-def month-hn-id)]          ;; hard-coded table in index.html
      (map (fn [pg-offset]
             ;; files are numbered off-by-one to match the page param on HN
             (pp/cl-format nil "files/~a/~a.html" month-hn-id (inc pg-offset)))
        (range (:pgCount mo-def)))
      (throw (js/Exception. (str "msg id " month-hn-id " not defined in gMonthlies table."))))))

(defn unprocessed-month [month-hn-id]
  {:month-hn-id          month-hn-id                        ;; FYI
   :month-load-complete? false
   :page-urls-remaining  (month-page-urls month-hn-id)
   :jobs-seen            #{}
   :athings              []
   :jobs                 []})

;;; --- kick off month load from initilaize-db or when user selects new month (see pick-a-month) -----

(rfr/reg-event-db :month-set
  (fn [db [_ month-hn-id]]
    (let [mo-def (get-monthly-def month-hn-id)]
      (println :month-setting month-hn-id)
      ;; job-list-loader div is subscribed to :month-load-task and
      ;; kicks off the first event...
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
    [:div                                                   ;; {:style {:display "none"}}
     (let [task @(rfr/subscribe [:month-load-task])]
       ;; once the potential job nodes have been scraped and saved
       ;; for processing, the saving event will alter :urls-to-scrape to its rest.
       ;; this will run again and make a new loader if any remain.
       (if (seq (:page-urls-remaining task))
         (do (println :mk-pg-loader-for (:page-urls-remaining task))
             [mk-page-loader task])
         ;; our work is done, tho we keep getting called
         ;; todo trim this bit
         (do (println :pages-no-mas)
             [:span "no mas pages"])))]))

;;; --- dev-time limits -----------------------------
;;; n.b.: these will be limits *per page*

(def ATHING-PARSE-MAX 500)
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
              :on-load #(let [ifr (.-target %)]
                          (rfr/dispatch [:page-athings-culled
                                         (utl/update-multi task
                                           [:page-urls-remaining rest]
                                           [:athings concat (job-page-athings ifr)])]))}]))

(rfr/reg-event-fx :page-athings-culled
  (fn [{:keys [db]} [_ task]]
    (println :culled-to-date! (count (get-in db [:month-load-task :athings])))

    (merge
      {:db (assoc db :month-load-task task)}
      (when (empty? (:page-urls-remaining task))
        {:dispatch [:cull-jobs-from-athings]}))))

(def ATHING_CHUNK_SZ 20)                                    ;; bigger chunks zoom due to 15ms clock; use small value to see progress bar working

(rfr/reg-event-fx :cull-jobs-from-athings
  (fn [{:keys [db]} [_]]
    (let [task (:month-load-task db)
          chunk (take ATHING_CHUNK_SZ (get-in db [:month-load-task :athings]))]
      (if (seq chunk)
        (let [new-jobs (filter #(:OK %) (map #(parse/job-parse % (:jobs-seen task)) chunk))]
          (println :cull-job-chunk (count chunk) :new (count new-jobs))
          {:db       (assoc db :month-load-task
                               (utl/update-multi task
                                 [:athings nthrest ATHING_CHUNK_SZ]
                                 [:jobs concat new-jobs]
                                 [:jobs-seen clojure.set/union (into #{} (map :hn-id new-jobs))]))
           :dispatch [:cull-jobs-from-athings]})
        (do (println :athings-exhausted)
            {:db (assoc db :month-load-complete? true)})))))

;;; --- sub jobs-filtered watches this ---------------------------------------

(rfr/reg-sub :month-load-complete?
  (fn [db]
    (get-in db [:month-load-task :month-load-complete?])))

;;; --- UI: month selecting by user ------------------------------------------

(defn pick-a-month []
  (let [months (gMonthlies-cljs)]
    [:div {:class "pickAMonth"}
     [:select {:class     "searchMonth"
               :value     (or @(rfr/subscribe [:month-hn-id]) "")
               :on-change #(do (println :bam-mo (.-value (.-target %)))
                               (rfr/dispatch [:month-set (.-value (.-target %))]))}
      (let []
        (map (fn [mno mo-def]
               (let [{:keys [hnId desc] :as all} mo-def]
                 ^{:key mno} [:option {:key hnId :value hnId} desc]))
          (range)
          months))]
     [:div {:style utl/hz-flex-wrap}
      [utl/view-on-hn {:hidden (nil? @(rfr/subscribe [:month-hn-id]))}
       (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" @(rfr/subscribe [:month-hn-id]))]
      [:span {:style  {:color  "#fcfcfc"
                       :margin "0 12px 0 12px"}
              :hidden (nil? @(rfr/subscribe [:month-hn-id]))}
       (str "Total jobs: " (count @(rfr/subscribe [:month-jobs])))]]]))

