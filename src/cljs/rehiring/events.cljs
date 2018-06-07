(ns rehiring.events
  (:require
   [re-frame.core :as rfr]
   [rehiring.db :as db]))

(rfr/reg-event-db
 ::initialize-db
 (fn [_ _]
   (db/initial-db)))

(rfr/reg-event-db
  ::month-set
  (fn [db [_ hn-id]]     ;; new-filter-kw is one of :all, :active or :done
    (assoc db :month-hn-id hn-id)))
