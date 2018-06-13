(ns rehiring.events
  (:require
   [re-frame.core :as rfr]
   [rehiring.db :as db]
   [rehiring.job-loader :as jbld]))


(rfr/reg-event-db ::toggle-show-job-details
  (fn [db [_ job-no]]
    ;(println ::toggle-show-job-details job-no (type job-no))
    ;(println :all-deets (:show-job-details db))
    (update-in db [:show-job-details job-no] not)))


;;; --- filtering -------------------------------------------------------

(rfr/reg-event-db
  :filter-activate
  (fn [db [_ tag active?]]
    (assoc-in db [:filter-active tag] active?)))

(rfr/reg-event-db
  :show-filtered-excluded-toggle
  (fn [db [_ active?]]
    (update db :show-filtered-excluded not)))

(rfr/reg-sub
  :show-filtered-excluded
  (fn [db]
    (:show-filtered-excluded db)))