(ns rehiring.events
  (:require
   [re-frame.core :as rfr]
   [rehiring.db :as db]
   [rehiring.job-scraper :as scraper]))

(rfr/reg-event-db
 ::initialize-db
 (fn [_ _]
   (db/initial-db)))

(rfr/reg-event-db
  ::month-set
  (fn [db [_ hn-id]]
    (assoc db :month-hn-id hn-id)))

(rfr/reg-event-db
  ::month-page-collect
  (fn [db [_ ifr-dom hn-id pg-no]]
    (println :replacing-jobs hn-id pg-no)
    (assoc db :jobs (scraper/jobs-collect ifr-dom))))

(rfr/reg-event-db
  ::toggle-show-job-details
  (fn [db [_ job-no]]
    (println ::toggle-show-job-details job-no (type job-no))
    (println :all-deets (:show-job-details db))
    (update-in db [:show-job-details job-no] not)))


