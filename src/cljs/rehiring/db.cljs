(ns rehiring.db
  (:require [clojure.walk :as walk]))

(def SEARCH-MO-STARTING-IDX 0)

(defn initial-db []
  (let [months (walk/keywordize-keys (js->clj js/gMonthlies))]
    {:months months
     :month-hn-id (:hnId (nth months SEARCH-MO-STARTING-IDX))
     :job-list-max 5}))