(ns rehiring.user-annotations
  (:require
    [goog.string :as gs]
    [reagent.core :as rgt]
    [re-frame.core :as rfr]
    [rehiring.utility :as utl]))


;;; --- utilities -----------------------------------

(defn db-unotes-ensure [db hn-id]
  (if (contains? (:user-notes db) hn-id)
    db (assoc-in db [:user-notes hn-id] {:hn-id hn-id
                                         :stars 0})))

(defn unote->local-storage
  [unote]
  (let [nkey (str utl/ls-key "-unotes-" (:hn-id unote))]
    (println :storing-unote (:hn-id unote) nkey unote)
    (.setItem js/localStorage nkey (str unote))))

;;; --- re-frame-ese ---------------------------------

(rfr/reg-sub :unotes
  (fn [db [_ hn-id]]
    (get-in db [:user-notes hn-id])))

(rfr/reg-sub :unotes-prop
  (fn [db [_ hn-id property]]
    (get-in db [:user-notes hn-id property])))

(rfr/reg-event-fx :unotes-prop-toggle
  (fn [{:keys [db]} [_ hn-id property]]
    (let [new-db (update-in (db-unotes-ensure db hn-id)
                   [:user-notes hn-id property] not)]
      {:db       new-db
       :dispatch [:persist-unote hn-id (get-in new-db [:user-notes hn-id])]})))

(rfr/reg-event-fx :unotes-prop-set
  (fn [{:keys [db]} [_ hn-id property new-value]]
    (let [new-db (assoc-in (db-unotes-ensure db hn-id)
                   [:user-notes hn-id property] new-value)]
      {:db       new-db
       :dispatch [:persist-unote hn-id (get-in new-db [:user-notes hn-id])]})))

(rfr/reg-event-fx :persist-unote
  (fn [cfx [_ hn-id unotes]]
    (unote->local-storage unotes)))

;;; --- components -----------------------------------

(def MAX-STARS 3)

(defn job-stars []
  (fn [job]
    [:div {:style utl/hz-flex-wrap-bottom}
     (let [j-stars (or @(rfr/subscribe [:unotes-prop (:hn-id job) :stars]) 0)]
       (for [sn (range MAX-STARS)]
         ^{:key sn} [:span {:style {:cursor "pointer"
                                    :color  (if (>= j-stars (inc sn)) "red" "gray")}
                            :on-click (fn [e]
                                         (println :bamchg-stars
                                           sn (.-checked (.-target e)))
                                         (rfr/dispatch [:unotes-prop-set (:hn-id job) :stars
                                                        (if (= sn (dec j-stars))
                                                          0 (inc sn))]))}
                     (gs/unescapeEntities "&#x2605;")]))]))

(defn note-toggle []
  (fn []
    [:p "note-toggle"]))

(defn note-editor []
  (fn []
    [:p "note-editor"]))

(defn applied [job]
  (fn [job]
    (let [input-id (str "applied?" (:hn-id job))]
      [:div {:style utl/hz-flex-wrap-centered}
       [:input {:id       input-id
                :type     "checkbox"
                :style    {:margin-left "18px"}
                :checked  @(rfr/subscribe [:unotes-prop (:hn-id job) :applied])
                :on-click #(rfr/dispatch [:unotes-prop-toggle (:hn-id job) :applied])}]
       [:label {:for   input-id
                :style {:color (if @(rfr/subscribe [:unotes-prop (:hn-id job) :applied])
                                 "red" "black")}}
        "Applied"]])))

(defn exclude-job [job]
  (fn [job]
    (let [excluded? @(rfr/subscribe [:unotes-prop (:hn-id job) :excluded])]
      [:span {:style    {:color       (if excluded? "red" "black")
                         :font-size   "1em"
                         :font-weight (if excluded? "bolder" "lighter")
                         :margin      "4px 4px 8px 0"}
              :on-click #(rfr/dispatch [:unotes-prop-toggle (:hn-id job) :excluded])}
       (gs/unescapeEntities "&#x20E0;")])))

;;; --- main ------------------------------------------------------


(defn user-annotations []
  (fn [job]
    [:div {:class "userAnnotations"}
     [job-stars job]
     [applied job]
     [note-toggle]
     [exclude-job job]
     [note-editor job]]))

