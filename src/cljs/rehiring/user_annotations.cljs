(ns rehiring.user-annotations
  (:require
    [goog.string :as gs]
    [reagent.core :as rgt]
    [re-frame.core :as rfr]
    [rehiring.utility :as utl]))

(defn job-stars []
  (fn []
    [:p "job-stars"]))

(defn applied []
  (fn []
    [:p "applied"]))

(defn note-toggle []
  (fn []
    [:p "note-toggle"]))
(defn note-editor []
  (fn []
    [:p "note-editor"]))

(declare exclude-job)

(defn user-annotations []
  (fn [job]
    [:div {:class "userAnnotations"}
     [job-stars job]
     [applied job]
     [note-toggle]
     [exclude-job job]
     [note-editor job]]))

(rfr/reg-sub :unotes
  (fn [db [_ hn-id]]
    (get-in db [:user-notes hn-id])))

(defn exclude-job [job]
  (fn [job]
    (let [excluded? @(rfr/subscribe [:unotes-prop (:hn-id job) :excluded])]
      (println :xjob-sees (:hn-id job) excluded?)
      [:span {:style    {:color       (if excluded? "red" "black")
                         :font-size   "1em"
                         :font-weight (if excluded? "bolder" "lighter")
                         :margin      "4px 4px 8px 0"}
              :on-click #(rfr/dispatch [:unotes-prop-toggle (:hn-id job) :excluded])}
       (gs/unescapeEntities "&#x20E0;")])))

(rfr/reg-sub
  :unotes-prop
  (fn [db [_ hn-id property]]
    (get-in db [:user-notes hn-id property])))

(rfr/reg-event-db
  :unotes-prop-toggle
  (fn [db [_ hn-id property]]
    (println :unotes-prop-toggle-ing hn-id (get-in db [:unotes-prop hn-id property]))
    (update-in db [:user-notes hn-id property] not)))
