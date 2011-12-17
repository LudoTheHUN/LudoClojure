(ns LudoClojure.timelooper1.timelooper1
 ; (:use LudoClojure.Win_scrachAlwaysRun)
  (:require calx)  ;;this means that I don't haveto have a copy of the calx source in src
 
  (:require clojure.contrib.duck-streams )   ;needed for serde
  (:require clojure.contrib.seq-utils)       ;needed for serde

  (:gen-class))
  
(import '(java.awt AWTException Robot Rectangle Toolkit GridLayout GridBagLayout GridBagConstraints Insets)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension)
        '(java.awt.event.ActionListener)
        '(javax.swing JPanel JFrame JSlider BoxLayout JLabel JButton SwingConstants)
        '(javax.swing.event ChangeListener ChangeEvent)
        '(java.lang Thread)
        )

;(:import [com.nativelibs4java.opencl CLContext CLByteBuffer CLMem CLMem$Usage CLEvent]
;         [com.nativelibs4java.util NIOUtils]
;        [java.nio ByteOrder ByteBuffer])


(use 'calx)
(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)


(defn serialize
  "Print a data structure to a file so that we may read it in later."
  [data-structure #^String filename]
  (clojure.contrib.duck-streams/with-out-writer
    (java.io.File. filename)
    (binding [*print-dup* true] (prn data-structure))))

;; This allows us to then read in the structure at a later time, like so:
(defn deserialize [filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))


(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))
  
  

(quote 

(defn -main [& args]
  (println "Its not easy enough to get started with Clojure programming")
  (println (hello args))
  (when (= args 1) (do (println "option1 selcted")))
  (when (= args nil) (do (println "no options")))
  
   
)

)


(println "hello there from timelooper1.clj")






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops32
;TODO close over the opencl code... or rather send it off onto another thread.?
;Note innner loop within openCL context can not be quit without loose hooks to openCL buffers??
;Can we onload and offload within this loop when it is setoff on another thread?
;To get the image to refresh , just need to close over (closure style) openCL code , passing in the veriables?? But longer term, the opencl should be on a separate thread, in ... have a listener ...look into sendof?

;TODO animate over growing moding values for random number geerator..., do it with slider...
;TODO Add a user interface, slider?, for the moding value.
     ;TODO DONE set a lay out strategy... DONE!
     ;TODO DONEconnect up action listeners
     ;TODO put openCL on a action listener dependet loop (events)	
;TODO recode GUI with ???  http://lifeofaprogrammergeek.blogspot.com/2009/05/model-view-controller-gui-in-clojure.html
;look at: http://kotka.de/blog/2010/03/proxy_gen-class_little_brother.html
;Yup, I'm crazy too :-)  http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout


(def sourceOpenCL
  "

int xpos(int nid, int xsize, int yzise)
{
return nid % xsize;
}

int ypos(int nid, int xsize, int yzise)
{
return nid / xsize;
}



  
__kernel void randomnumbergen(
    __global int *output_i,
    __global int *output_mz,
    __global float *neuron_prop1a,
    __global float *neuron_prop1b,
    const int kernelloopsize,
    const int kernelrandmoderoutput
    )
{
    int gid = get_global_id(0);
    int gid2 = get_global_id(0);
    int gsize = get_global_size(0);
    int gsize_by_2 = gsize / 2;

    int iatom;
    int synapse_from_nid;
    float neuron_prop1 = 0.0 ;
    
    
    
    uint m_z = gid + 1;  //random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms
    uint m_w = 1;
    
    
    barrier(CLK_LOCAL_MEM_FENCE);

    for(iatom = 0; iatom < kernelloopsize; iatom+=1 )
    {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);

        int output_i_id = (gid2 * kernelloopsize) + iatom;
        uint random_value = ((m_z << 16) + m_w)  % 50;
        
        output_i[output_i_id] = random_value;      //This is the random number being created. Note the mod operation %, this makes it easy to create a random in some range...
        
        if (random_value <= 25) {
        neuron_prop1 += neuron_prop1 + neuron_prop1a[random_value];
        }
        if (random_value > 40) {
        neuron_prop1 += neuron_prop1 - neuron_prop1a[random_value];
        }
        
        //Any writing out of bounds of an array corrupts memory further afield!!!
        // Note: ANY writing to the same location causes a catastrophic result fail, aka race condition
        
        
        //kernelrandmoderoutput is the second slider...
        //output_mz[gid*kernelloopsize + iatom ] = m_z;
        //Further neuron code goes within this loop. This loop is per neuron, each random number + some transform function on the random number is the proceduraly generated LSM synapse to resultand neuron pointers.
        //Will need reducer like logic... with probably +=
        //synapse_from_nid = ((m_z << 16) + m_w)  % gsize;
        //neuron_prop1 = neuron_prop1 + neuron_prop1a[synapse_from_nid];   //need a sygmoid here + inhibatory connections
        
    }
    barrier(CLK_LOCAL_MEM_FENCE);
    neuron_prop1b[gid] = (1.0 / (1.0 + exp( -1.0 * neuron_prop1))) +0.0000001;
    barrier(CLK_LOCAL_MEM_FENCE);

}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))

(def neuron_prop1_clj (atom [1]))


;m_w = <choose-initializer>;    /* must not be zero */
;m_z = <choose-initializer>;    /* must not be zero */
; 
;uint get_random()
;{
;    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
;    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
;    return (m_z << 16) + m_w;  /* 32-bit result */
;}
(def gloal_size_clj (* 512))
;TODO make UI and openCL run on one global simulation size.
(def kernelloopsize_clj (atom 1))
(def kernelrandmoderoutput_clj (atom 14))
(def innerloop_clj (atom 5))

(def runInnerOpenCL (atom false))
(def run_reder_lock (atom true))
(swap! runInnerOpenCL (fn [_] true))
(swap! innerloop_clj (fn [_] 2000000))
(def scanscreen (atom 1))

;;TODO Too many atoms?? should these not be just def/refs?

(quote threading work

win9test
(def foo 12)
(def foo {:a 1 :b 3})
foo   ;this is the root binding
(defn printfoo 
   ([] (printfoo ""))  ;arity 0, calls self with arity 1
   ([prefix]  (println prefix foo @kernelloopsize_clj)))
   
(printfoo)
(printfoo "boo")
(defn addfoo [x y] (+ x y))

(binding [foo 3] ;;binding macro , changes the value of foo within the scope of the binding only and within the thread from which binding is calledS, foo has not changed
  (printfoo))
(printfoo)   ;;outside foo is still its original

(defn with-new-thread [f]
 (.start (Thread. f)))
  
(with-new-thread 
   (fn [] (do
             (Thread/sleep 3000)
             (println "new thread")
             nil   ;NOTE nil is needed else we get a null pointer expression
             )))
  
  
(with-new-thread 
   (fn []  (printfoo "new thread")))

(do (binding [foo "JO!"]
      (with-new-thread (fn []  (printfoo "bound background")))   ;;values should be root binding, ie: the map
      (printfoo "bound foreground")     ;;value should be the bound values, JO!
      )
    (printfoo "unbound foreground")    ;;values should be the root biding against.. the map.
)
(swap! kernelloopsize_clj (fn [] 3))

(defn dothisstuff [f ms_sleeptime iterations]
(with-new-thread (fn [] 
                   (loop [k iterations]
                      (do 
                        (eval f)
                        (Thread/sleep ms_sleeptime)
                        (println "on a thread in itteration " k)
                      (if (= k 1) nil (recur (dec k) )))))))
                      
(dothisstuff 1 100 10)
(dothisstuff (printfoo "unbound foreground") 100 10)


WIP!!
(defn with-new-thread-foreverslowly [f ms_sleeptime iterations]
 (.start (Thread. (fn [f ms_sleeptime iterations]
                    (loop [k iterations]
                      (do 
                        f
                        (Thread/sleep ms_sleeptime))
                      (if (= k 1) nil (recur (dec k) ))))
 
                      )))
                      

(with-new-thread-foreverslowly 1 100 10) 
(with-new-thread-foreverslowly (printfoo "unbound foreground") 100 10)            
(with-new-thread-foreverslowly (fn [] (printfoo "unbound foreground")) 100 10)


;;should we do this with an agent that never returns?

(defn with-new-thread [f & args]
 (.start (Thread. (apply f args))))   ;TODO MAKE THIS VERIALE ARITY

(with-new-thread 
   (fn [x] (do
             (Thread/sleep x)
             (println "new thread" x)
             nil
             )) 1000)
             
((fn [x] (do
             (Thread/sleep x)
             (println "new thread")
             nil
             )) 1000)

             
(def runInnerOpenCL (atom false))
(swap! runInnerOpenCL (fn [_] true))

(defmacro forever [& body] 
  `(while true (do
                 ~@body
                (Thread/sleep 1)  ;;Adding this wait time makes this not suck up the whole CPU... it does only 1000 things can be passed in per second.
                )))
  
(.start (Thread.
          (fn [] (forever (while (= @runInnerOpenCL true)
           (do
             (println "I was waiting all that time")
             (swap! runInnerOpenCL (fn [_] false))
             (Thread/sleep 1)))))   ;This is bad, without the sleep it just heats up the CPU
             ;making it stop
))
  

);END OF quoted block

(.start (new Thread (fn [] (println "Hello" (Thread/currentThread)))))

;;TODO  looking to using macros to abstact these components out. (creating buffers, main loop, onloading, offloading)
(defn runCL []      ;;TODO DONE:this function is a quick copout to get the visualisation to work, I want to be inside a persistant inner openCL loop (in the middle below) so that the whole openCL machiery doesn't have to be restarted each time...
(swap! runInnerOpenCL (fn [_] true))
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
          InnerLoopCount innerloop_clj   ;;InnerLoopCount points to the top level atom innerloop_clj, thus it has the same value as the top level atom...
          kernelloopsize kernelloopsize_clj    ;note these are atoms holding the value.... this may be a very bad patern?
          kernelrandmoderoutput kernelrandmoderoutput_clj]

        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
          (def Buffer_int1     (create-buffer (* gloal_size 1000 2) :int32  ))
          (def Buffer_int2     (create-buffer (* gloal_size 1000 2) :int32  ))
          (def neuron_prop1a   (create-buffer gloal_size :float32  ))
          (def neuron_prop1b   (create-buffer gloal_size :float32  ))
          (enqueue-barrier) (finish)
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
          (loop [k @InnerLoopCount]
;TODO Have data come in from the outside here, inside this loop, but conditionally, ie: only if the outside has make a change, also do not load (and let the old value of the Buffer be used)
                  (if (= true @runInnerOpenCL) ;Not happy with this, would be nice to use something like a watch , will refactor eventually...
                  ;;TODO Look for a more lisner like mechanisim rather then infinate slowed down loop....
                    (let
                      [inbuffer1 (wrap @OpenCLoutputAtom1 :int32)]
                      (enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2 neuron_prop1a neuron_prop1b @kernelloopsize @kernelrandmoderoutput)
                      (enqueue-barrier)(finish)   ;Could load in a huge buffer a bloack at a time...
                      ;;(Thread/sleep 10)  
                      (enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2 neuron_prop1b neuron_prop1a @kernelloopsize @kernelrandmoderoutput)
                      (enqueue-barrier)(finish)   ;Could load in a huge buffer a bloack at a time...                      
                      ;;;Note, this is where out 'double buffer' will go, we'll keep swapping the state back and forth between 2 sets of buffers, each update is one time tick.
                      (finish)
                      (println "Done openCL inner work, countdowns left: " k)
                  
;TODO Have the readout be done conditionally when needed...
                      (def startnanotime_bufferReadOutTime (. System (nanoTime)))
                      (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 gloal_size])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
                      (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 gloal_size])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
                     ;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
                      (enqueue-barrier)(finish)
                      ;;(Thread/sleep 20)   
                      (swap! neuron_prop1_clj (fn [foo] (^floats float-array (deref (enqueue-read neuron_prop1b [0 gloal_size])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
                      (enqueue-barrier)(finish)
                      ;;(Thread/sleep 20)   
                      (def endnanotime_bufferReadOutTime (. System (nanoTime)))
                      (swap! runInnerOpenCL (fn [_] false))
                   )
                   (do
                    ;(println "Skipped openCL inner work, countdowns left: " k)
                    (Thread/sleep 1)
                   )
                 )
            (if (= k 1) nil (recur (dec k) )))      ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))

        (enqueue-barrier) 
        (finish)
        (release! Buffer_int1)
        (release! Buffer_int2)
        (enqueue-barrier)
        (finish)
    nil)
  ))
)

(.start (new Thread runCL))




;(println (vec @OpenCLoutputAtom1))
;(println (vec @OpenCLoutputAtom2))
;(println (vec @OpenCLoutputAtom3))

(defn print_openCLtests []

(println(count @OpenCLoutputAtom1))
(println(reduce + @OpenCLoutputAtom1) )
(println(/ (reduce + @OpenCLoutputAtom1) (count @OpenCLoutputAtom1) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj @innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
        ))

(nth @OpenCLoutputAtom1 0)
)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;pixels per world cell
(def scale 5)
(def dim 120)
(def counter (atom 0))


(defn render [g]   ;note: 'g' is ?????
  (def startnanotime_rendertime (. System (nanoTime)))
  (let [img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
   ;Screen clearing
   (if (= @scanscreen 0)
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    )
    (doto bg
      (.setColor (. Color blue))
      (.drawRect 10 20 30 40)
      (.drawLine 15 (* 3 @counter) (* 2 @counter) @counter))
;TODO rip this out into a function call if possible without messing about with the bg oject too much
    (dorun    ;;this draws many one pixel lines, no point drawing method :-/
      (for [y (range (* 1 )) ] 
       (dorun
         (for [x (range (- gloal_size_clj 1)) ]
            (doto bg
              ;(.setColor (. Color red))
              (.setColor (new Color (mod @scanscreen 255) (mod x 255) (mod (nth @OpenCLoutputAtom1 x) 255)))
              (.drawLine  
                          (+ y @scanscreen) ;;;NOTE becuase this atom is called twice, sometimes it gets swaped while the renderer runs, hence UI line artifacts when a the sliders already submited a new job while drawline is looping over the point range.
                          (+ (nth @OpenCLoutputAtom1 x) 50)
                          (+ y @scanscreen)
                          (+ (nth @OpenCLoutputAtom1 x) 50 y)

                          ))))))
    
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))
    )
  (def endnanotime_rendertime (. System (nanoTime)))
  ;(println "To Render, it takes ms:" (/ (- endnanotime_rendertime startnanotime_rendertime ) 1000000.0))
)
;(map  (fn [x] (nth @OpenCLoutputAtom1 x))(range 0 gloal_size_clj))



(def label (JLabel. "Counter: 0"))

(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g))
                        )   ;;'paint'  is a method inhereted from JComponent
                 (.setPreferredSize (new Dimension      ; '.setPreferredSize'  is also a method inhereted from JComponent
                                     (* scale dim)
                                     (* scale dim)))
                ; (.setLayout (new GridBagLayout ))
                ; (.add label
                ;    (GridBagConstraints. 0 1 1 1 1.0 1.0 (GridBagConstraints/WEST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
                ; (.add button
                ;    (GridBagConstraints. 1 1 1 1 1.0 1.0 (GridBagConstraints/WEST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
                 ))


                 
(defn reder_loop []
(swap! run_reder_lock (fn [_] false))
(loop [k @innerloop_clj]
;TODO Have data come in from the outside here, inside this loop, but conditionally, ie: only if the outside has make a change, also do not load (and let the old value of the Buffer be used)
                 (if (= true @run_reder_lock) ;Not happy with this, would be nice to use something like a watch , will refactor eventually...
                  ;;TODO Look for a more lisner like mechanisim rather then infinate slowed down loop....
                   (do 
                     (swap! run_reder_lock (fn [_] true))
                     ;(println @scanscreen)
                     (swap! scanscreen (fn [x] (mod (inc x) 500)))
                     (.repaint panel)
                     (Thread/sleep 20)
                   )
                   (do
                     ;(println "Skipped openCL inner work, countdowns left: " k)
                     (Thread/sleep 1)   
                   )
                 )
            (if (= k 1) nil (recur (dec k) )))
)

(.start (new Thread reder_loop))


;;TODO Put panel repain on a listener like loop too...  make it wait for runInnerOpenCL = false (but then next one could already have started...), take a copy of the contents of the atom, and visualise that... (but then that could be not inline with data in the cloders)... ok for delayed visualisation... slider should force a draw...

(def slider (doto (proxy [JSlider ] [1 500 5])
                  (.addChangeListener  (proxy [ChangeListener] []    
                                         (stateChanged [evt]
                                           (let [val (.. evt getSource getValue)]
                                            (do (.setText label
                                                  (str "Slider: " val))                  ;do 1st thing 
                                                (swap! counter (fn [_] val))
                                                (swap! kernelloopsize_clj (fn [_] val))
                                               ;; (swap! kernelrandmoderoutput_clj (fn [_] val))   ;;; Moved this to the second slider
                                                (swap! runInnerOpenCL (fn [_] true)) ;;This used to be (runCL), but now we just saw we want the work to get done, it will be done within 50ms... 
;TODO Have the repaint happen on a different thread, continuously, with 50ms waits...
                                                (swap! run_reder_lock (fn [_] true))
                                                (Thread/sleep 10)
                                                (println (map  (fn [x] (nth @neuron_prop1_clj x))(range 0 100)))
                                                
                                                ;(.repaint panel)                         ;do 2nd thing
                                                (println "slider got moved to value:" @counter )) ;do 3rd thing
                                                ))))
            ; (.setPreferredSize (new Dimension (* scale 20)(* scale 40)))
            ;(.setConstraints (. GridBagConstraints HORIZONTAL))
            ))

            
            
            
;;;This block also works whilst being outside of the slider definition.... shows that listeners can be added whereever...
;(.addChangeListener slider (proxy [ChangeListener] []   
;                            (stateChanged [evt]
;                            (let [val (.. evt getSource getValue)]
;                             (.setText label
;                                (str "Slider: " val))))))	

(def slider2 (doto (proxy [JSlider] [1 500 5] )
                   ;(.setPreferredSize (new Dimension (* 10)(* 100)))    ;;Example constraints on layout...
                   ;(.setOrientation (. SwingConstants VERTICAL))        ;;Example of setting something, note SwingConstants had to be imported
                   (.addChangeListener  (proxy [ChangeListener] []    
                                         (stateChanged [evt]
                                           (let [val (.. evt getSource getValue)]
                                            (do (swap! kernelrandmoderoutput_clj (fn [_] val))
                                                (swap! runInnerOpenCL (fn [_] true)) ;;This used to be (runCL), but now we just saw we want the work to get done, it will be done within 50ms... 
                                                (swap! run_reder_lock (fn [_] true))
                                                ;(.repaint panel)                         ;do 2nd thing
                                                (println "vertical slider got set to :" val )) ;do 3rd thing
                                                ))))
              ))

(defmacro on-action [component event & body]
  ;;How ^{:doc "macro designed to make adding action listeners trivial from http://stuartsierra.com/2010/01/03/doto-swing-with-clojure"}
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(def button (doto (JButton. "Add 1")
                  (on-action evnt  ;; evnt is not used here
                    (.setText label (str "Counter: " (swap! counter inc)))
                    (swap! run_reder_lock (fn [_] true))
                    ;(.repaint panel)
                    (println "action jbuton click happened"))
                ;This is the macro expanded equivalent
                  ;(.addActionListener        
                  ;  (proxy [java.awt.event.ActionListener] []
                  ;    (actionPerformed [evt] 
                  ;      (do (.setText label (str "Counter: " (swap! counter inc)))
                  ;          (.repaint panel)
                  ;          (println "action jbuton click happened2")))))
            ))






;http://thinkrelevance.com/blog/2008/08/12/java-next-2-java-interop.html
;(def c (new GridBagConstraints))
;(set! (. c gridx) 3)
;(set! (. c gridy) GridBagConstraints/RELATIVE)



(defn frame []     ;;This is called from the core as entry point the UI and program
        (swap! runInnerOpenCL (fn [_] true)) ;;This used to be (runCL), but now we just say we want the work to get done, it will be done within 50ms...     ;; This initiates everything openCL wise just before it is needed by the UI for the first time
        (doto 
             ;(new JFrame "GridBagLayout Test" )   ;what is the difference between these?
             (proxy [JFrame] ["OpenCL  Multiply-with-carry Random number explorer"])
             
             
             (.setLayout (new GridBagLayout ))

             ;;This is what made it finally clear to see: http://infernus.org/2010/08/swinging-clojure/
             (.add panel  ;(new GridBagConstraints (. c gridx)  )
                  (GridBagConstraints. 0 0 2 1 1.0 1.0 (GridBagConstraints/WEST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
               ;;;java interop Syntax eye opener!!!
             ;(.setLayout panel (new GridBagLayout ))
             (.add slider 
                  (GridBagConstraints. 0 1 1 1 1.0 1.0 (GridBagConstraints/EAST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
             (.add slider2 
                  (GridBagConstraints. 1 1 1 1 1.0 1.0(GridBagConstraints/EAST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
             (.add label
                  (GridBagConstraints. 1 2 1 1 1.0 1.0 (GridBagConstraints/WEST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
             (.add button
                  (GridBagConstraints. 0 2 1 1 1.0 1.0 (GridBagConstraints/WEST) (GridBagConstraints/BOTH) (Insets. 4 4 4 4) 0 0))
             ;(.add label)
             ;(.add button)
             
            ; (.setLayout (new GridBagLayout )) 
              ; (def [c (new GridBagConstraints)]
               ;(set! (. c gridx) 1)
              ; (set! (. c gridy) 3))
             ;(.setLayout (new GridLayout 2 1))
             (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)   ;This is need for uberjar to be able to continue after the close event....how could this be done headlessly??
             .pack 
             .show))


(println "I'm at the end of timelooper1")
(quote
;(.setLayout frame (GridBagLayout.)) ;; 
;(.setLayout slider (GridBagLayout.))
;(.setLayout slider2 (GridBagLayout.))
;(.setLayout panel (GridBagLayout.))

;http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout

;(.setLayout frame (BoxLayout.))

;(. panel (repaint))

;Total OpenCL kereltime in ms: 1715.679627
;number operations per second: 11.734509685  Bln
;LudoClojure.core=> (* 64 64 64 64 12)
;201326592        

;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);


)


(in-ns 'LudoClojure.timelooper1.timelooper1)

(println (map  (fn [x] (nth @neuron_prop1_clj x))(range 0 100)))

(quote ;some alternatvies....

(def vec1 (atom []))


(swap! vec1 (fn [x] (conj x (atom [1]))))

(swap! (nth @vec1 5) (fn [x] 8))

;;Does this scale + paralelize... how well...
)





