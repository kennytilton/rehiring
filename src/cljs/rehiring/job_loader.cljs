(ns rehiring.job-loader
  (:require
    [clojure.walk :as walk]
    [rehiring.db :as rhdb]
    [rehiring.events :as evt]
    [rehiring.subs :as subs]
    [re-frame.core :as rfr]
    [cljs.pprint :as pp]))

;; --- loading job data -----------------------------------------

(defn monthlies-kw []
  (walk/keywordize-keys (js->clj js/gMonthlies)))

(defn pick-a-month []
  (let [months (monthlies-kw)]
    [:div {:class "pickAMonth"}
     [:select {:class     "searchMonth"
               :value     (:hnId (nth months rhdb/SEARCH-MO-STARTING-IDX))
               :on-change (fn [e]
                            (let [opt (.-target e)
                                  hnid (.-value opt)]
                              (println "Hello onchange" (js->clj opt) hnid)
                              (rfr/dispatch [::evt/month-set hnid])))}
      (let []
        (map (fn [mno mo-def]
               (let [{:keys [hnId desc] :as all} mo-def]
                 [:option {:key hnId :value hnId}
                  desc]))
          (range)
          months))]]))

(declare mk-page-loader)

(defn job-listing-loader []
  (fn []
    [:div {:style {:display "none"}}
     (let [selId @(rfr/subscribe [:month-hn-id])
           moDef (some (fn [mo]
                         (when (= (:hnId mo) selId)
                           mo))
                   (monthlies-kw))]
       (assert moDef)
       ;;(println :modef (:pgCount moDef) moDef)

       (if (pos? (:pgCount moDef))
         (doall (map (fn [pgn]
                       ^{:key (str selId "-" (inc pgn))} [mk-page-loader selId (inc pgn)])
                  (range (:pgCount moDef))))
         [mk-page-loader selId]))]))

(defn mk-page-loader []
  (fn [hn-id pg-no]
    (let [src-url (pp/cl-format nil "files/~a/~a.html" hn-id (or pg-no hn-id))]
      (println :mkpg hn-id pg-no src-url)
      [:iframe {:src     src-url
                :on-load #(let [ifr (.-target %)]
                            ;;(println "Loaded!!" ifr hn-id pg-no src-url)
                            (rfr/dispatch [::evt/month-page-collect ifr hn-id pg-no]))}])))

