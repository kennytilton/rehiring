(ns rehiring.job-list
  (:require [re-frame.core :as rfr]
            [rehiring.events :as evt]
            [rehiring.filtering :as flt]
            [goog.string :as gs]
            [cljs.pprint :as pp]))

(declare job-header job-details)

(defn job-list-sort [jobs] jobs)

(defn jump-to-hn [hn-id]
  (.open js/window (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" hn-id) "_blank"))

(defn job-list-item []
  (fn [job-no job]
    [:li {:style    {:cursor     "pointer"
                     :padding    "12px"
                     :background (if (zero? (mod job-no 2))
                                   "#eee" "#f8f8f8")}
          ;;:on-click #(rfr/dispatch [::evt/toggle-show-job-details job-no])
          }
     [job-header job]
     [job-details job]]))

(defn job-list []
  (fn []
    [:ul {:style {:list-style-type "none"
                  :background      "#eee"
                  :padding         0
                  :margin          0}}
     (map (fn [jn j]
            ^{:key (:hn-id j)} [job-list-item jn j])
       (range)
       ;; todo sexify
       (let [raw-jobs @(rfr/subscribe [:jobs])
             sel-jobs (flt/job-list-filter raw-jobs)]
         (take @(rfr/subscribe [:job-display-max])
           (job-list-sort sel-jobs))))]))

(defn user-annotations []
  (fn [job]
    [:span (str "unotes " (:company job))]))

(rfr/reg-sub
  :job-collapse-all
  (fn [db [_ hn-id]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (:job-collapse-all db)))

(defn job-details []
  (fn [job]
    (let [deets-raw @(rfr/subscribe [:show-job-details (:hn-id job)])
          deets (if (nil? deets-raw) true deets-raw)]
      ;;(println :deets (:company job) deets (type (:hn-id job)))
      [:div {:class (if deets "slideIn" "slideOut")
             :style {:margin     "6px"
                     :background "#fff"
                     :display    (if deets "block" "none")}}
       [user-annotations job]
       [:div {:style           {:margin   "6px"
                                :overflow "auto"}
              :on-double-click #(jump-to-hn (:hn-id job))}
        (when (and (not @(rfr/subscribe [:job-collapse-all]))
                   @(rfr/subscribe [:show-job-details (:hn-id job)]))
          (map (fn [x node]
                 (case (.-nodeType node)
                   1 ^{:key (str (:hn-id job) "-p-" x)} [:p (.-innerHTML node)]
                   3 ^{:key (str (:hn-id job) "-p-" x)}[:p (.-textContent node)]
                   ^{:key (str (:hn-id job) "-p-" x)}
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
      (gs/unescapeEntities "&#x2b51")]
     [:span {
             ;;:on-click #(rfr/dispatch [::evt/toggle-show-job-details (:hn-id job)])
             }
      (:title-search job)]]))

