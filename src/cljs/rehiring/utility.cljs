(ns rehiring.utility
  (:require
    [goog.string :as gs]
    [re-frame.core :as rfr]))

(defn slide-in-anime [ show?]
  (if show? "slideIn" "slideOut"))

(def hz-flex-wrap {:display "flex"
                   :flex-wrap "wrap"})

(def ls-key "rehiring-browser")                         ;; localstore key

(def hz-flex-wrap-centered
  {:display "flex"
   :flex-wrap "wrap"
   :align-items "center"})

(def hz-flex-wrap-bottom
  {:display "flex"
   :flex-wrap "wrap"
   :align-items "bottom"})

(defn unesc [entity]
  (gs/unescapeEntities entity))

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

(defn view-on-hn []
  (fn [attrs uri]
    [:a (merge {:href uri, :title "View on the HN site"} attrs)
     [:img {:src "dist/hn24.png"}]]))

(defn help-list [helpItems helping]
  (fn []
    [:div {:class    (str "help " (slide-in-anime @helping))
           :style    {:display (if @helping "block" "none")
                      :margin-top 0
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