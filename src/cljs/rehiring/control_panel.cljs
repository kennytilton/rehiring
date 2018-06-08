(ns rehiring.control-panel
  (:require [rehiring.filtering :as flt]
            [rehiring.utility :as utl]
            [re-frame.core :as rfr]))

(declare job-listing-control-bar)

(defn control-panel []
  (fn []
    [:div { :style {:background "#ffb57d"}}
      ;[open-shut-case "os-filters" "Filters" true flt/mk-title-selects]
     [flt/mk-title-selects]
     [flt/mk-user-selects]
     [job-listing-control-bar]]))

(defn job-listing-control-bar []
  (fn []
    [:div {:style {:display "flex"
                   :flex-wrap "wrap"
                   :align-items "center"
                   :margin "12px 0 0 0px"
                   :font-size "1em"
                   :padding "4px"
                   :border-style "groove"
                   :border-color "khaki"
                   :border-width "2px"
                   :background "PAPAYAWHIP"
                   :justify-content "space-between"}}
     [:div {:style utl/hz-flex-wrap-centered}
      [:span {:style {:font-size "1em"
                      :margin-right "12px"}}
       (let [jobs @(rfr/subscribe [:jobs])]
         (str "Jobs: " (count jobs)))]]]))



;
;function jobListingControlBar() {
;           return div({
;                       style: merge( hzFlexWrapCentered, {
;                                                          margin:"12px 0 0 0px"
;                                                                 , font_size: "1em"
;                                                                 , padding: "4px"
;                                                                 , border_style: "groove"
;                                                                 , border_color: "khaki"
;                                                                 , border_width: "2px"
;                                                                 , background: "PAPAYAWHIP"
;                                                                 , justify_content: "space-between"
;                                                                 , align_items: "center"})}
;
;,div( { style: merge( hzFlexWrapCentered, {flex_wrap:"wrap"})}
;, span({ style: "font-size:1em;margin-right:12px"
;                , content: cF(c => "Jobs: " + c.md.fmUp("job-list").selectedJobs.length)})
;, span( {
;         style: cF( c=> "padding-bottom:4px;cursor:pointer;display:flex;align-items:center;font-size:1em;visibility:" + (c.md.excludedCt > 0 ? "visible;" : "hidden;") +
;                    "border:" + (c.md.onOff ? "thin solid red;":"none;"))
;, content: cF( c=> "&#x20E0;: "+ c.md.excludedCt)
;                , onclick: md => md.onOff = !md.onOff
;                , title: "Show/hide items you have excluded"
;         }, {
;             name: "showExcluded"
;                   , onOff: cI( false)
;, excludedCt: cF( c=> c.md.fmUp("job-list").selectedJobs.filter( j=> UNote.dict[j.hnId].excluded).length)
;             }))
;, resultMax()
;, button({
;          style: cF(c=> {
;                         let pgr = c.md.fmUp("progress")
;                             return "font-size:1em; min-width:96px; display:"+ (pgr.hidden? "block":"none")
;                         })
;, onclick: mx => {
;                  let all = document.getElementsByClassName('listing-toggle');
;                      Array.prototype.map.call(all, tog => tog.onOff = !mx.expanded)
;                      mx.expanded = !mx.expanded
;                  }
;
;, content: cF( c=> c.md.expanded? "Collapse All":"Expand All")
;          }
;, { name: "expander", expanded: cI( !mobileCheck() )}))
;                                 }
