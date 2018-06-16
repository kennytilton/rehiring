(ns rehiring.job-loader
  (:require
    [rehiring.utility :as utl]
    [rehiring.job-parse :as parse]

    [re-frame.core :as rfr]
    [cljs.pprint :as pp]
    [clojure.string :as str]))


;;; --- initially, this sub sees any month id set by initialize DB --------



;;; --- second, make an iframe and process each url in turn -----------------
;; we do not want to load everything into multiple iframes at once because RAM.
;;
;; A key sub-plot is that we want to do a progress bar, so we need the
;; max of the bar established by culling all aThings from all pages before starting to process.
;; (Finding the athings is very fast.)

(declare mk-page-loader job-page-athings)

(defn job-listing-loader
  "The main player in loading a month. It spawns one child loader whenever there is a URL to be scraped."
  []
  (fn []
    (println :jll-building!)
    [:div                                                   ;; {:style {:display "none"}}
     (if-let [page-url (first @(rfr/subscribe [:urls-to-scrape]))]
       ;; once the potential job nodes have been scraped and saved
       ;; for processing, the saving event will alter :urls-to-scrape to its rest.
       ;; this will run again and make a new loader if any remain.
       (do (println :mk-pg-loader-for page-url)
           [mk-page-loader page-url])
       (do (println :pages-no-mas)
           [:span "no mas pages"]))]))

(defn mk-page-loader []
  (fn [src-url]
    [:iframe {:src     src-url
              :on-load #(let [ifr (.-target %)]
                          (rfr/dispatch [:page-athings-culled (job-page-athings ifr)]))}]))

;;; --- dev-time limits -----------------------------
;;; n.b.: these will be limits *per page*

(def ATHING-PARSE-MAX 10)
(def JOB-LOAD-MAX 10000)                                    ;; todo: still needed?

(defn job-page-athings [ifr-dom]
  (when-let [cont-doc (.-contentDocument ifr-dom)]
    (let [hn-body (aget (.getElementsByTagName cont-doc "body") 0)]
      (let [a-things (take ATHING-PARSE-MAX (prim-seq (.querySelectorAll hn-body ".athing")))]
        (set! (.-innerHTML hn-body) "")                     ;; free up memory
        a-things))))

(rfr/reg-event-fx :page-athings-culled
  (fn [{:keys [db]} [_ athings]]
    (println :culled! (count athings) (:urls-to-scrape db))

    (let [out (merge
                {:db (reduce (fn [db op]                    ;; todo make this a utiity
                               (apply update db op))
                       db [[:month-athings concat athings]
                           [:urls-to-scrape rest]])         ;; this kicks off jll to make a new iframe
                 }
                (when (nil? (rest (:urls-to-scrape db)))
                  {:dispatch [:cull-jobs-from-athings]}))]
      (println :ot-keys (keys out))
      (println :culled-out (select-keys (:db out) [:month-hn-id :urls-to-scrape ]))
      out)))

;;; --- OK, all athings are loaded ----------------------
;;; The progress bar max has been tracking :month-athings.
;;; Now it can start tracking :month-jobs for the progress
;;; jobs-list can :month-jobs-parsed to know when to populate

(def ATHING_CHUNK_SZ 20)                                    ;; bigger chunks zoom due to 15ms clock; use small value to see progress bar working

(rfr/reg-event-fx :cull-jobs-from-athings
  (fn [{:keys [db]} [_]]
    (let [chunk (subvec (:month-athings db) 0 ATHING_CHUNK_SZ)]
      (if (seq chunk)
        (do (println :cull-job-chunk (count chunk))
            {:db       (reduce (fn [db op]
                                 (apply update db op))
                         db [[:month-jobs concat (filter #(:OK %) (map parse/job-parse chunk))]
                             [:month-athings subvec ATHING_CHUNK_SZ]])
             :dispatch [:cull-jobs-from-athings]})
        (do (println :athings-exhausted)
            {:db (assoc db :month-jobs-parsed true)})))))


;;; --- sub jobs-filtered watches this ---------------------------------------

(rfr/reg-sub :month-jobs-parsed
  (fn [db]
    (:month-jobs-parsed db)))

;;; --- UI: month selecting by user ------------------------------------------

(defn pick-a-month []
  (let [months (utl/gMonthlies-cljs)]
    [:div {:class "pickAMonth"}
     [:select {:class     "searchMonth"
               :value     @(rfr/subscribe [:month-hn-id])
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

;;; --- kick off a new month load when user selects new month-----

(rfr/reg-event-db :month-set
  (fn [db [_ month-hn-id]]
    (let [mo-def (utl/get-monthly-def month-hn-id)]
      (println :month-setting month-hn-id)
      (merge db (utl/unprocessed-month month-hn-id)))))