(ns rehiring.job-list
  (:require [re-frame.core :as rfr]
            [rehiring.events :as evt]
            [goog.string :as gs]
            [cljs.pprint :as pp]))

(declare job-header job-details)

(defn job-list-filter [jobs] jobs)                          ;; todo

(defn job-list-sort [jobs] jobs)

(defn jump-to-hn [hn-id]
  (.open js/window (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" hn-id) "_blank"))



(defn job-list-item []
  (fn [job-no job]
    (println :keyyyyyyyyyyyy (:hn-id job) (:company job))
    [:li {:key      (:hn-id job)
          :style    {:cursor     "pointer"
                     :padding    "12px"
                     :background (if (zero? (mod job-no 2))
                                   "#eee" "#f8f8f8")}
          :on-click (rfr/dispatch [::evt/toggle-show-job-details job-no])}
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
             sel-jobs (job-list-filter raw-jobs)]
         (take @(rfr/subscribe [:job-list-max])
           (job-list-sort sel-jobs))))]))

(defn user-annotations []
  (fn [job]
    [:span (str "unotes " (:company job))]))

(defn job-details []
  (fn [job]
    (let [deets-raw @(rfr/subscribe [:show-job-details (:hn-id job)])
          deets (if (nil? deets-raw) true deets-raw)]
      (println :deets (:company job) deets)
      [:div {:class (if deets "slideIn" "slideOut")
             :style {:margin     "6px"
                     :background "#fff"
                     :display    (if deets "block" "none")}}
       [user-annotations job]
       [:div {:style           {:margin   "6px"
                                :overflow "auto"}
              :on-double-click #(jump-to-hn (:hn-id job))}
        (when deets
          (map (fn [node]
                 (case (.-nodeType node)
                   1 [:p (.-innerHTML node)]
                   3 [:p (.-textContent node)]
                   (str "<p> Unexpected node type = " (.-nodeType node) "</p>")))
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
     [:span {:on-click #(rfr/dispatch [::evt/toggle-show-job-details (:hn-id job)])}
      (:title-search job)]]))

