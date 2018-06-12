(ns rehiring.db
  (:require [clojure.walk :as walk]
            [re-frame.core :as rfr]
            [clojure.string :as str]
            [rehiring.utility :as utl]))

(def SEARCH-MO-STARTING-IDX 0)

(defn initial-db []
  (let [months (walk/keywordize-keys (js->clj js/gMonthlies))]
    {:months                months
     :month-hn-id           (:hnId (nth months SEARCH-MO-STARTING-IDX))
     :job-collapse-all      false
     :toggle-details-action "expand"
     :job-display-max       3                               ;; todo restore to 42
     :job-sort (nth utl/job-sorts 0)
     :show-filters true
     :show-job-details      {}                              ;; key is hnId, value t/f; handle default in view
     }))

(rfr/reg-event-fx ::initialize-db
  [(rfr/inject-cofx :local-store-unotes)]

  (fn [{:keys [local-store-unotes]} _]
    {:db (assoc (initial-db) :user-notes local-store-unotes)}))

(declare ls-get-wild)

(rfr/reg-cofx
  :local-store-unotes
  (fn [cofx _]
    (assoc cofx :local-store-unotes
                (ls-get-wild (str utl/ls-key "-unotes-") :hn-id))))

(defn io-all-keys []
  (println :js/local-keys (.keys js/Object js/localStorage))
  (.keys js/Object (.-localStorage js/window)))

(defn ls-get-wild [prefix key-property]
  (let [allk (io-all-keys)]
    (into {}
      (remove nil?
        (for [lsk allk]
          (do
            (when (and (str/starts-with? lsk prefix)
                       (> (count lsk) (count prefix)))

              [(subs lsk (count prefix))
               (cljs.reader/read-string
                 (.getItem js/localStorage lsk))])))))))

(defn io-find [key-prefix]
  (loop [keys (io-all-keys)
         found []]
    (if (seq keys)
      (recur (rest keys)
        (if (str/starts-with? (first keys) key-prefix)
          (conj found (first keys))
          found))
      found)))
