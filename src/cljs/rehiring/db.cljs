(ns rehiring.db
  (:require [clojure.walk :as walk]
            [re-frame.core :as rfr]
            [clojure.string :as str]
            [rehiring.utility :as utl]))

(def INITIAL-SEARCH-MO-IDX 1)                               ;; handy when debugging specific month

(defn initial-db []
  (let [months (utl/gMonthlies-cljs)]
    {;; :months                months
     :month-hn-id            (:hnId (nth months INITIAL-SEARCH-MO-IDX))
     :job-collapse-all       false
     :toggle-details-action  "expand"
     :job-display-max        42
     :job-sort               (nth utl/job-sorts 0)
     :show-filters           true
     :show-filtered-excluded false
     :rgx-match-case         false
     :rgx-xlate-or-and       true
     :search-history         {}
     :show-job-details       {}
     }))

(defn io-all-keys []
  (.keys js/Object (.-localStorage js/window)))

(defn ls-get-wild
  "Loads all localStorage values whose key begins with
  prefix into a dictionary, using the rest of the LS key
   as the dictionary key."
  [prefix]

  (into {}
    (remove nil?
      (for [lsk (io-all-keys)]
        (when (and (str/starts-with? lsk prefix)
                   ;; ugh, we got some garbage in LS
                   ;; may as well create permanent filter
                   (> (count lsk) (count prefix)))
          [(subs lsk (count prefix))                        ;; toss prefix
           (cljs.reader/read-string
             (.getItem js/localStorage lsk))])))))

(rfr/reg-cofx :storage-user-notes
  ;; load user notes from local storage
  ;; todo: name rectification
  ;; one glitch in naming: a user can have multiple notes on one job,
  ;; so I ended up with plurals at two levels. "unotes" are a collection
  ;; of notes on *one job*. "user-notes" are the collection of "unotes".
  ;;
  (fn [cofx _]
    (let [notes (ls-get-wild (str utl/ls-key "-unotes-"))]
      (assoc cofx
        :storage-user-notes
        (ls-get-wild (str utl/ls-key "-unotes-"))))))

(rfr/reg-event-fx ::initialize-db
  [(rfr/inject-cofx :storage-user-notes)]

  (fn [{:keys [storage-user-notes]} _]
    {:db (assoc (initial-db) :user-notes storage-user-notes)}))

