(ns rehiring.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :month-hn-id
 (fn [db]
   (:month-hn-id db)))

(re-frame/reg-sub
  :jobs
  (fn [db]
    (:jobs db)))

(re-frame/reg-sub
  :job-list-max
  (fn [db]
    (:job-list-max db)))

(re-frame/reg-sub
  :show-job-details
  (fn [db hn-id]
    (get-in db [:show-job-details hn-id])))
