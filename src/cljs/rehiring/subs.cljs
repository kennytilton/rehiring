(ns rehiring.subs
  (:require
   [re-frame.core :as rfr]))

(rfr/reg-sub :month-hn-id
 (fn [db]
   (:month-hn-id db)))

(rfr/reg-sub :jobs
  (fn [db]
    (:jobs db)))

(rfr/reg-sub :job-sort
  (fn [db]
    (:job-sort db)))

(rfr/reg-event-db :job-sort-set
  (fn [db [_ new-sort]]
    (assoc db :job-sort new-sort)))

(rfr/reg-sub :show-job-details
  (fn [db [_ hn-id]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (get-in db [:show-job-details hn-id])))
