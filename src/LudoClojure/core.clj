(ns LudoClojure.core
  (:use LudoClojure.Win_scrachAlwaysRun)
  (:require calx)  ;;this means that I don't haveto have a copy of the calx source in src
 
  (:require clojure.contrib.duck-streams )   ;needed for serde
  (:require clojure.contrib.seq-utils)       ;needed for serde

  (:gen-class))
  
(import '(java.awt AWTException Robot Rectangle Toolkit GridLayout GridBagLayout)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension)
		'(javax.swing JPanel JFrame JSlider BoxLayout)
        )
		
;(:import [com.nativelibs4java.opencl CLContext CLByteBuffer CLMem CLMem$Usage CLEvent]
;	     [com.nativelibs4java.util NIOUtils]
;	     [java.nio ByteOrder ByteBuffer])
		
		
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
  
  

(defn -main [& args]
  (println "Its not easy enough to get started with Clojure programming")
  (println (hello args))
)


(println "hello there from core.clj file")





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops28
;TODO animate over growin moding values for random number geerator...
;TODO Add a user interface, slider?, for the moding value.
;TODO recode GUI with ??  http://lifeofaprogrammergeek.blogspot.com/2009/05/model-view-controller-gui-in-clojure.html

(def sourceOpenCL
  "

__kernel void randomnumbergen(
    __global uint *output_i,
    __global uint *output_mz,
	const unsigned int kernelloopsize
	)
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);

int iatom;
uint m_z = gid + 1;  //random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms
uint m_w = 1;

for( iatom = 0; iatom < kernelloopsize; iatom+=1 )
    {
    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
    m_w = 18000 * (m_w & 65535) + (m_w >> 16);

    output_i[gid*kernelloopsize + iatom ] = ((m_z << 16) + m_w)  % 14;      //This is the random number being created. Note the mod operation %, this makes it easy to create a random in some range...
	output_mz[gid*kernelloopsize + iatom ] = m_z;
	
	//Further neuron code goes within this loop. This loop is per neuron, each random number + some transform function on the random number is the proceduraly generated LSM synapse to resultand neuron pointers.
	//Will need reducer like logic... with probably with +=
	
    }
	
	
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))



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
(def kernelloopsize_clj 1)
(def innerloop_clj 1)

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
        InnerLoopCount innerloop_clj
		kernelloopsize kernelloopsize_clj]
        
        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
          (def Buffer_int1   (create-buffer (* gloal_size kernelloopsize) :int32  ))
		  (def Buffer_int2   (create-buffer (* gloal_size kernelloopsize) :int32  ))
          (enqueue-barrier) (finish)		
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
          (loop [k InnerLoopCount]
                (do
                        (enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2 kernelloopsize)   
                        (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						;;;Note, this is where out 'double buffer' will go, we'll keep swapping the state back and forth between 2 sets of buffers, each update is one time tick.
                        (finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))
        
        (def startnanotime_bufferReadOutTime (. System (nanoTime)))
        
          (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
          (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
          ;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
        (def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
        (release! Buffer_int1)
        (enqueue-barrier)
        (finish)
    nil)))

(println (vec @OpenCLoutputAtom1))
(println (vec @OpenCLoutputAtom2))
;(println (vec @OpenCLoutputAtom3))

(println(count @OpenCLoutputAtom1))
(println(reduce + @OpenCLoutputAtom1) )
(println(/ (reduce + @OpenCLoutputAtom1) (count @OpenCLoutputAtom1) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
        ))

(nth @OpenCLoutputAtom1 0)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;pixels per world cell
(def scale 5)
(def dim 120)

(defn render [g]   ;note: 'g' is 
  (let [img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (doto bg
	 (.setColor (. Color blue))
	 (.drawRect 10 20 30 40)
	 (.drawLine 15 15 15 15))
	 
	 (dorun 
      (for [x (range 511) ]
	    (doto bg
		  (.setColor (. Color red))
	      (.drawLine  (+ (nth @OpenCLoutputAtom1 x) 50)
		              x 
					  (+ (nth @OpenCLoutputAtom1 x) 50)
					  x))))
	
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))
	))

	
(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim)
                                     (* scale dim)))
			 		 
									 ))

(def slider (doto (proxy [JSlider] []
                        ;(paint [g] (render g))
						)
            ; (.setPreferredSize (new Dimension (* scale 20)(* scale 40)))
			;(.setConstraints (. GridBagConstraints HORIZONTAL))
									 ))
									 
									 
(def frame (doto 
             (new JFrame)
			 (.add slider)
			 (.add panel)
			 .pack 
			 .show))

(.setLayout frame (GridBagLayout.))

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




