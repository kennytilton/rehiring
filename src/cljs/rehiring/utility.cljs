(ns rehiring.utility
  (:require
    [goog.string :as gs]
    [re-frame.core :as rfr]
    [clojure.walk :as walk]
    [cljs.pprint :as pp]))

(def ls-key "rehiring-browser")                             ;; localstore key

(defn gMonthlies-cljs []
  ;; gMonthlies defined in index.html for extensibility
  (walk/keywordize-keys (js->clj js/gMonthlies)))

(defn get-monthly-def [hn-id]
  (some (fn [mo]
          (println :get-mo hn-id (type hn-id) (type (:hnId mo)))
          (when (= (:hnId mo) hn-id)
            (println :mo-bam mo)
            mo))
    (gMonthlies-cljs)))

(defn unprocessed-month [month-hn-id]
  {:month-hn-id   month-hn-id                               ;; triggers whole load process
   ;;; --- first, compute the resource file URLs to be scraped ---------------

   :urls-to-scrape (let [urls (when month-hn-id
                                (println :mo month-hn-id)
                    (if-let [mo-def (get-monthly-def month-hn-id)] ;; hard-coded table in index.html
                      (do (println :def mo-def)
                      (map (fn [pg-offset]
                             ;; files are numbered off-by-one to match the page param on HN
                             (pp/cl-format nil "files/~a/~a.html" month-hn-id (inc pg-offset)))
                        (range (:pgCount mo-def))))
                      (throw (js/Exception. (str "msg id " month-hn-id " not defined in gMonthlies table.")))))]
                     (println :scraping urls)
                     urls)

   :month-athings []                                        ;; first we grab all nodes from all pages, to decide "max" of HTML progress element
   :job-ids-seen  #{}                                       ;; de-duper (pulling extra pages keeps returning the last)
   :month-jobs    []                                        ;; end result
   })

;;; --- handy CSS --------------------------------------------

(defn slide-in-anime [show?]
  (if show? "slideIn" "slideOut"))

(def hz-flex-wrap {:display   "flex"
                   :flex-wrap "wrap"})

(def hz-flex-wrap-centered
  {:display     "flex"
   :flex-wrap   "wrap"
   :align-items "center"})

(def hz-flex-wrap-bottom
  {:display     "flex"
   :flex-wrap   "wrap"
   :align-items "bottom"})

(defn unesc [entity]
  (gs/unescapeEntities entity))

;;; --- sorting ---------------------------------------------

(defn job-company-key [j]
  (or (:company j) ""))

(defn job-stars-enrich [job]
  (assoc job :stars (or @(rfr/subscribe [:unotes-prop (:hn-id job) :stars]) 0)))

(defn job-stars-compare [dir j k]
  ;; force un-starred to end regardless of sort order
  ;; order ties by ascending hn-id

  (let [j-stars (:stars j)
        k-stars (:stars k)]

    (let [r (if (pos? j-stars)
              (if (pos? k-stars)
                (* dir (if (< j-stars k-stars)
                         -1
                         (if (> j-stars k-stars)
                           1
                           (if (< (:hn-d j) (:hn-id k)) -1 1))))
                -1)
              (if (pos? k-stars)
                1
                (if (< (:hn-d j) (:hn-id k)) -1 1)))]
      r)))

(def job-sorts [{:title "Creation" :key-fn :hn-id :order -1}
                {:title "Stars" :comp-fn job-stars-compare :order -1 :prep-fn job-stars-enrich}
                {:title "Company" :key-fn job-company-key :order 1}])


;;; --- re-usable widgetry ----------------------------------------------

(defn view-on-hn []
  (fn [attrs uri]
    [:a (merge {:href uri, :title "View on the HN site"} attrs)
     [:img {:src "dist/hn24.png"}]]))

(defn help-list [helpItems helping]
  (fn []
    [:div {:class    (str "help " (slide-in-anime @helping))
           :style    {:display     (if @helping "block" "none")
                      :margin-top  0
                      :padding-top "6px"}
           :on-click (fn [mx]
                       (reset! helping false))}
     [:div {:style {:cursor      "pointer"
                    :textAlign   "left"
                    :marginRight "18px"}}
      [:ul {:style {:listStyle  "none"
                    :marginLeft 0}}
       (map (fn [ex e]
              ^{:key ex} [:li {
                               :style {:padding 0
                                       :margin  "0 18px 9px 0"}}
                          [:div {:dangerouslySetInnerHTML {:__html e}}]])
         (range)
         helpItems)]]]))

(defn toggle-char [db-key title on-char off-char attrs style]
  (fn []
    (let [on-off @(rfr/subscribe [db-key])]
      [:span (merge {:style    (merge {:font-weight "bold"
                                       :cursor      "pointer"
                                       :margin-left "9px"
                                       :font-family "Arial"
                                       :font-size   "1em"} style)
                     :title    title
                     :on-click #(rfr/dispatch [:toggle-key db-key])}
               attrs)
       (unesc (if on-off on-char off-char))])))

(defn open-shut-case [toggle-db-key title & cases]
  (fn []
    [:div
     [:div {:class "selector"}
      [:span title]
      [toggle-char toggle-db-key
       (str "Show/hide " title)
       "&#x25be", "&#x25b8" {} {}]]

     [:div {:class "osBody"
            :style {:background "#ff6600"
                    :display    (if @(rfr/subscribe [toggle-db-key]) "block" "none")}}
      (for [case cases]
        ^{:key (rand-int 100000)} (case))]]))