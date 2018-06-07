(ns rehiring.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :month-hn-id
 (fn [db]
   (:month-hn-id db)))
