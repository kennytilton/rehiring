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


