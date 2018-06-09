(ns rehiring.subs
  (:require
   [re-frame.core :as rfr]))

(rfr/reg-sub
 :month-hn-id
 (fn [db]
   (:month-hn-id db)))

(rfr/reg-sub
  :jobs
  (fn [db]
    (:jobs db)))

(rfr/reg-sub
  :job-display-max
  (fn [db]
    (:job-display-max db)))

(rfr/reg-sub
  :show-job-details
  (fn [db [_ hn-id]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (get-in db [:show-job-details hn-id])))
