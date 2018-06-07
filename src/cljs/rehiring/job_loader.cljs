(ns rehiring.job-loader
  (:require
    [clojure.walk :as walk]
    [rehiring.db :as rhdb]
    [rehiring.events :as evt]
    [rehiring.subs :as subs]
    [re-frame.core :as rfr]))

;; --- loading job data -----------------------------------------

(defn pick-a-month []
  (let [months (walk/keywordize-keys (js->clj js/gMonthlies))]
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

(defn job-listing-loader []
  (fn []
    (let [hn-id @(rfr/subscribe [:month-hn-id])]
      [:p (str "loader of " hn-id)])))

;           pickAMonth() {
;                       return div ({ class: "pickAMonth"}
;, select( {
;           name: "searchMonth"
;                 , class: "searchMonth"
;                 , value: cI( gMonthlies[SEARCH_MO_IDX].hnId)
;, onchange: (mx,e) => {
;                       let pgr = mx.fmUp("progress")
;                           ast(pgr)
;                           pgr.value = 0
;                       pgr.maxN = 0
;                       pgr.seen = new Set()
;                       pgr.hidden = false
;                       mx.value = e.target.value
;                       }}
;          // --- use this if complaints about initial load ----
;          // , option( {value: "none"
;                        //         , selected: "selected"
;                        //         , disabled: "disabled"}
;                       //     , "Pick a month. Any month.")
;  , gMonthlies.map( (m,x) => option( {
;                                      value: m.hnId
;                                             , selected: x===SEARCH_MO_IDX? "selected":null}
;  , m.desc)))
;
;, div( {style: hzFlexWrapCentered}
;, viewOnHN( cF( c=> `https://news.ycombinator.com/item?id=${c.md.fmUp("searchMonth").value}`)
;, { hidden: cF( c=> !c.md.fmUp("searchMonth").value)})
;, span({
;        style: "color: #fcfcfc; margin: 0 12px 0 12px"
;               , hidden: cF( c=> !c.md.fmUp("searchMonth").value)
;, content: cF(c => {
;                    let pgr = c.md.fmUp("progress")
;, jobs = c.md.fmUp("jobLoader").jobs || [];
;                    return pgr.hidden ? "Total jobs: " + jobs.length
;                    : "Parsing: "+ PARSE_CHUNK_SIZE * pgr.value})})
;
;, progress({
;            max: cF( c=> c.md.maxN + "")
;, hidden: cF( c=> !c.md.fmUp("searchMonth").value)
;                 , value: cI(0)
;            }, {
;                name: "progress"
;                      , maxN: cI(0)
;, seen: cI( new Set())})
;
;            ))
;                       }
;
