(ns rehiring.job-listing-control-bar
  (:require [re-frame.core :as rfr]
            [rehiring.utility :as utl]))

(declare result-max jobs-all-expansion)

(defn job-listing-control-bar []
  (fn []
    (println :rebuilding-job-listing-control-bar)
    [:div {:class "listingControlBar"}
     ;;; --- match count---------------------------------------------------
     [:div {:style utl/hz-flex-wrap-centered}
      [:span {:style {:font-size    "1em"
                      :margin-right "12px"}}
       (let [jobs @(rfr/subscribe [:jobs])]
         (str "Jobs: " (count jobs)))]]

     [result-max]
     [jobs-all-expansion]]))

;;; --- display limit ------------------------------------------------

(defn result-max []
  (fn []
    [:div {:style (merge utl/hz-flex-wrap-centered {:margin-right "6px"})}
     [:span "Show:"]
     (let [rmax @(rfr/subscribe [:job-display-max])]
       [:input {:type      "number"
                :value     rmax

                :on-change #(let [new (.-value (.-target %))]
                              (println "raw new" new (js/parseInt new))
                              (rfr/dispatch [:set-result-display-max (js/parseInt new)]))

                :style     {:font-size    "1em"
                            :max-width    "48px"
                            :margin-left  "6px"
                            :margin-right "6px"
                            }}])]))
(rfr/reg-sub
  :job-display-max
  (fn [db]
    (:job-display-max db)))

(rfr/reg-event-db
  :set-result-display-max
  (fn [db [_ rmax]]
    (println :new-rmax->db rmax)
    (assoc db :job-display-max rmax)))


;;; --- expand/collapse all jobs ---------------------------------------

(defn jobs-all-expansion []
  (fn []
    [:button {:style    {:font-size "1em"
                         :min-width "128px"
                         ;; display after jobs loaded todo
                         }
              :on-click #(rfr/dispatch [:toggle-details-visibility-all])}
     (case @(rfr/subscribe [:toggle-details-action])
       "collapse" "Collapse all"
       "expand" "Expand all"
       "hunh all")]))

(rfr/reg-sub
  :toggle-details-action
  (fn [db]
    (:toggle-details-action db)))

(rfr/reg-event-db
  :toggle-details-visibility-all
  (fn [db [_ hn-id]]
    (let [new-deets (into {} (for [hn-id (map :hn-id (:jobs db))]
                               [hn-id (= "expand" (:toggle-details-action db))]))]
      (merge db {:toggle-details-action (case (:toggle-details-action db)
                                          "collapse" "expand"
                                          "expand" "collapse")
                 :show-job-details      new-deets}))))
