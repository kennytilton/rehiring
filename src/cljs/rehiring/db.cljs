(ns rehiring.db
  (:require [clojure.walk :as walk]))

(def SEARCH-MO-STARTING-IDX 0)

(defn initial-db []
  (let [months (walk/keywordize-keys (js->clj js/gMonthlies))]
    {:months months
     :month-hn-id (:hnId (nth months SEARCH-MO-STARTING-IDX))
     :job-collapse-all false
     :toggle-details-action "expand"
     :job-display-max 3 ;; todo restore to 42
     :show-job-details {} ;; key is hnId, value t/f; handle default in view
     }))