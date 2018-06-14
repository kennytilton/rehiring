(ns rehiring.job-listing-control-bar
  (:require [re-frame.core :refer [subscribe] :as rfr]
            [rehiring.utility :as utl]))

(rfr/reg-sub :jobs-filtered-excluded
  ;
  ; This is weird. jobs-filtered includes excluded jobs so we can let
  ; the user show/hide them while browsing a selection. This state then
  ; lets us tell them how many jobs matched by their search are hidden
  ; because the user has excluded them. Indeed, we might even be able
  ; to have this do no more than compute the count.
  ;
  ;; signal fn
  (fn [query-v _]
    [(subscribe [:jobs-filtered])
     (subscribe [:user-notes])])

  ;; compute
  (fn [[jobs-filtered user-notes]]
    (println :jfilex-sees (count jobs-filtered) (count user-notes))
    (filter (fn [j]
              (get-in user-notes [(:hn-id j) :excluded]))
      jobs-filtered)))



;;; --- display limit ------------------------------------------------

(defn result-max []
  (fn []
    [:div {:style (merge utl/hz-flex-wrap-centered {:margin-right "6px"})}
     [:span "Show:"]
     (let [rmax @(rfr/subscribe [:job-display-max])]
       [:input {:type         "number"
                :defaultValue rmax

                :on-key-press #(when (= "Enter" (js->clj (.-key %)))
                                 (rfr/dispatch [:set-result-display-max (js/parseInt (.-value (.-target %)))]))

                :on-blur      #(let [new (.-value (.-target %))]
                                 (println "blur new" new (js/parseInt new))
                                 (rfr/dispatch [:set-result-display-max (js/parseInt new)]))

                :style        {:font-size    "1em"
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

;;; --- the beef --------------------------------------------------

(defn job-listing-control-bar []
  (fn []
    (let [excluded @(rfr/subscribe [:jobs-filtered-excluded])]
      [:div {:class "listingControlBar"}
       ;;; --- match count---------------------------------------------------
       [:div {:style utl/hz-flex-wrap-centered}
        [:span {:style {:font-size    "1em"
                        :margin-right "12px"}}
         (let [jobs @(rfr/subscribe [:jobs-filtered])]
           (str "Jobs: " (count jobs)))]

        [:span {:style    {:padding-bottom "4px"
                           :cursor         "pointer"
                           :display        "flex"
                           :align-items    "center"
                           :font-size      "1em"
                           :visibility     (if (pos? (count excluded)) "visible" "hidden")
                           :border         (if @(rfr/subscribe [:show-filtered-excluded])
                                             "thin solid red" "none")
                           :title          "Show/hide items you have excluded"}
                :on-click #(rfr/dispatch [:show-filtered-excluded-toggle])
                }
         (str (utl/unesc "&#x20E0;") ": " (count excluded))]]

       [result-max]
       [jobs-all-expansion]])))

