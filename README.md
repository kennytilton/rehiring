# rehiring

My ["AskHN: Who Is Hiring"](https://github.com/kennytilton/whoshiring) browser ported to [re-frame](https://github.com/Day8/re-frame). Search and annotate the hundreds of jobs listed every month.

If yer just lookin' for work, a live version is [hosted here](https://kennytilton.github.io/whoishiring/) grace a GitHub. Or you can clone this and run it yourself. See below for a devops necessity.

## Development Mode

### Grab HN Pages
The app runs off pages curl'ed when you decide straight from the HN server. We start by visiting HN and tracking down the "Who Is Hiring?" question under `AskHN`. Here is [June, 2018](https://news.ycombinator.com/item?id=17205865). Now checkout the browser URL:
````
https://news.ycombinator.com/item?id=17205865
````
The browser app lives and breathes that `17205865`.

Now we want to pull 1-N pages under that ID, because when the list gets big HN breaks up the page into multiples keyed by a simple `p` parameter. Here is what we curl to get page 2:
````
https://news.ycombinator.com/item?id=17205865&p=2
````
To grab that:
````bash
cd rehiring
./grab 17205865
````
An optional second parameter tells the script to wait that many minutes and refresh, until control-C'ed.

If we curl too high a `p` (by setting MAX_P too high) we just get the latest, so I have not figured out a way to script a loop that stops whwn it has them all. So <sob> I edit the `grab` shell script and look for:
````bash
MAX_P=3
````
...and tweak the 3 (or whatever). Now edit `index.html` and look for this:
````js
    <script>
    var gMonthlies = [
        { hnId: "17205865", desc: "June, 2018", pgCount: 1}
        , { hnId: "16967543", desc: "May, 2018", pgCount: 6}
        , { hnId: "16735011", desc: "April, 2018", pgCount: 5}
    ]
    rehiring.core.init();
   </script>
````
Edit reasonably. The one mistake you can make (blame IFRAME!) is having a `pgCount` higher than the number of pages available: the app will wait for all pages to load but the extras of course will not load and using a JS timer to decide when to give up is left as an exercise. (No, IFRAMEs offer no error.)

Now your copy of the app should work with any new content you specify in `index.html`.

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
# rehiring
