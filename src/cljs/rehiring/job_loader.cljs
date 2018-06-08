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
    [:div {:style {:visibility "visible" #_"collapsed"}}
     (let [selId @(rfr/subscribe [:month-hn-id])
           moDef (some (fn [mo]
                         (when (= (:hnId mo) selId)
                           mo))
                   (monthlies-kw))]
       (assert moDef)
       (println :modef (:pgCount moDef) moDef)

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
                            (println "Loaded!!" ifr hn-id pg-no src-url)
                            (rfr/dispatch [::evt/month-page-collect ifr hn-id pg-no]))}])))

;function mkPageLoader( par, hnId, pgNo) {
;                                         return iframe({
;                                                        src: cF(c => {
;                                                                      if  (hnId === null) {
;                                                                                           return " "
;                                                                                           } else if ( pgNo === undefined) {
;
;                                                                return `files/${hnId}/${hnId}.html`
;    } else {
;            return `files/${hnId}/${pgNo}.html`
;    }
;            })
;, style: " display: none "
;, onload: md => jobsCollect(md)
;                                                        }
;, {
;   jobs: cI( null)
;, pgNo: pgNo
;   }


;, jobs: cF(c => {
;                 let parts = c.md.kids.map(k => k.jobs);
;                     if (parts.every(p => p !== null)) {
;                                                        //clg('all jobs resolved!!!!', parts.map( p => p.length))
;                                                        let all = parts.reduce((accum, pj) => {
;                                                                                               return accum.concat(pj)
;                                                                                               });
;                                                             return all;
;                                                        } else {
;                                                                return null
;                                                                }
;                 }, {
;                     observer: (s,md,newv) => {
;                                               if ( newv) {
;                                                           md.fmUp(" progress ").hidden = true;


;           pickAMonth() {
;                       return div ({ class: " pickAMonth "}
;, select( {
;           name: " searchMonth "
;                 , class: " searchMonth "
;                 , value: cI( gMonthlies[SEARCH_MO_IDX].hnId)
;, onchange: (mx,e) => {
;                       let pgr = mx.fmUp(" progress ")
;                           ast(pgr)
;                           pgr.value = 0
;                       pgr.maxN = 0
;                       pgr.seen = new Set()
;                       pgr.hidden = false
;                       mx.value = e.target.value
;                       }}
;          // --- use this if complaints about initial load ----
;          // , option( {value: " none "
;                        //         , selected: " selected "
;                        //         , disabled: " disabled "}
;                       //     , " Pick a month. Any month. ")
;  , gMonthlies.map( (m,x) => option( {
;                                      value: m.hnId
;                                             , selected: x===SEARCH_MO_IDX? " selected ":null}
;  , m.desc)))
;
;, div( {style: hzFlexWrapCentered}
;, viewOnHN( cF( c=> `https://news.ycombinator.com/item?id=${c.md.fmUp(" searchMonth ").value}`)
;, { hidden: cF( c=> !c.md.fmUp(" searchMonth ").value)})
;, span({
;        style: " color: #fcfcfc; margin: 0 12px 0 12px"
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
