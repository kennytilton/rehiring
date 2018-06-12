(ns rehiring.subs
  (:require
   [re-frame.core :as rfr]))

(rfr/reg-sub :month-hn-id
 (fn [db]
   (:month-hn-id db)))

(rfr/reg-sub :jobs
  (fn [db]
    (:jobs db)))

;(reg-sub
;  :filtered-jobs
;
;  ;; Signal Function
;  ;; Tells us what inputs flow into this node.
;  ;; Returns a vector of two input signals (in this case)
;  (fn [query-v _]
;    [(subscribe [:todos])
;     (subscribe [:showing])])
;
;  ;; Computation Function
;  (fn [[todos showing] _]   ;; that 1st parameter is a 2-vector of values
;    (let [filter-fn (case showing
;                      :active (complement :done)
;                      :done   :done
;                      :all    identity)]
;      (filter filter-fn todos))))

(rfr/reg-sub :show-job-details
  (fn [db [_ hn-id]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (get-in db [:show-job-details hn-id])))
