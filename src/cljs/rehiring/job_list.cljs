(ns rehiring.job-list
  (:require [re-frame.core :as rfr]
            [rehiring.events :as evt]
            [rehiring.filtering :as flt]
            [rehiring.user-annotations :as unt]
            [goog.string :as gs]
            [cljs.pprint :as pp]
            [rehiring.utility :as utl]))

(declare job-header job-details)

(defn job-list-sort [jobs]
  (let [{:keys [key-fn comp-fn order prep-fn]} @(rfr/subscribe [:job-sort])]
    (sort (fn [j k]
            (if comp-fn
              (comp-fn order j k)
              (* order (if (< (key-fn j) (key-fn k)) -1 1))))
      (map (or prep-fn identity) jobs))))

(defn jump-to-hn [hn-id]
  (.open js/window (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" hn-id) "_blank"))

(defn job-list-item []
  (fn [ job]
      [:li {:class "jobli"
            :style {:cursor     "pointer"
                    :display    (let [excluded @(rfr/subscribe [:unotes-prop (:hn-id job) :excluded])]
                                  (if (and excluded
                                           (not @(rfr/subscribe [:show-filtered-excluded]))
                                           (not @(rfr/subscribe [:filter-active "Excluded"])))
                                    "none" "block"))
                    :padding    "12px"}}
       [job-header job]
       [job-details job]]))

(defn job-list []
  (fn []
    [:ul {:style {:list-style-type "none"
                  :background      "#eee"
                  ;; these next defeat gratuitous default styling of ULs by browser
                  :padding         0
                  :margin          0}}
     (doall (map (fn [ j]
                   ^{:key (:hn-id j)} [job-list-item  j])

              (take @(rfr/subscribe [:job-display-max])
                (job-list-sort @(rfr/subscribe [:jobs-filtered])))))]))

(defn job-details []
  (fn [job]
    (let [deets @(rfr/subscribe [:show-job-details (:hn-id job)])]
      [:div {:class (if deets "slideIn" "slideOut")
             :style {:margin     "6px"
                     :background "#fff"
                     :display    (if deets "block" "none")}}
       [unt/user-annotations job]
       [:div {:style           {:margin   "6px"
                                :overflow "auto"}
              :on-double-click #(jump-to-hn (:hn-id job))}
        (when (and (not @(rfr/subscribe [:job-collapse-all]))
                   deets)
          (map (fn [x node]
                 (case (.-nodeType node)
                   1 ^{:key (str (:hn-id job) "-p-" x)} [:p (.-innerHTML node)]
                   3 ^{:key (str (:hn-id job) "-p-" x)} [:p (.-textContent node)]
                   ^{:key (str (:hn-id job) "-p-" x)} ;; todo try just x
                   [:p (str "Unexpected node type = " (.-nodeType node))]))
            (range)
            (:body job)))]])))

(defn job-header []
  (fn [job]
    [:div {:style    {:cursor  "pointer"
                      :display "flex"}
           :on-click #(rfr/dispatch [::evt/toggle-show-job-details (:hn-id job)])}
     [:span {:style {:color        "gray"
                     :max-height   "16px"
                     :margin-right "9px"
                     :display      "block"}}
      (utl/unesc "&#x2b51")]
     [:span {
             ;;:on-click #(rfr/dispatch [::evt/toggle-show-job-details (:hn-id job)])
             }
      (:title-search job)]]))

