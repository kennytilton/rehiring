(ns rehiring.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub :month-hn-id
 (fn [db]
   (:month-hn-id db)))

(reg-sub :job-sort
  (fn [db]
    (:job-sort db)))

(reg-sub :show-job-details
  (fn [db [_ hn-id]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (get-in db [:show-job-details hn-id])))

(reg-sub :toggle-key
  (fn [db [_ db-key]]
    (get db db-key)))

(reg-sub :show-filtered-excluded
  (fn [db]
    (:show-filtered-excluded db)))

(reg-sub :show-filters
  (fn [db]
    (:show-filters db)))