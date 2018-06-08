(ns rehiring.job-list
  (:require [re-frame.core :as rfr]
            [rehiring.events :as evt]
            [cljs.pprint :as pp]))

(defn job-list-filter [jobs] jobs)                          ;; todo

(defn job-list-sort [jobs] jobs)

(defn jump-to-hn [hn-id]
  (.open js/window (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" hn-id) "_blank"))

(defn job-header []
  (fn [job]
    [:div {:style {:cursor "pointer"
                   :display "flex"}
           :on-click #(rfr/dispatch [::evt/toggle-show-details (:hn-id job)])}
     [:span {:on-click #(rfr/dispatch [::evt/toggle-show-details (:hn-id job)])}
      (:title-search job)]]))

(defn job-details []
  (fn [job]
    [:p (str "details " (:company job))]))

;; ^{:key (str selId "-" (inc pgn))}

(defn job-list-item []
  (fn [job-no job]
    (println :keyyyyyyyyyyyy (:hn-id job) (:company job))
    [:li {:key (:hn-id job)
          :style {:cursor "pointer"
                  :padding "12px"
                  :background (if (zero? (mod job-no 2))
                                "#eee" "#f8f8f8")}
          :on-click (rfr/dispatch [::evt/toggle-show-details job-no])}
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
