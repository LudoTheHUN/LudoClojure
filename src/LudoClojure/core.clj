(ns LudoClojure.core
  (:use LudoClojure.Win_scrachAlwaysRun)
  (:require calx)  ;;this means that I don't haveto have a copy of the calx source in src
 
  (:require clojure.contrib.duck-streams )   ;needed for serde
  (:require clojure.contrib.seq-utils)       ;needed for serde

  (:gen-class))
  
(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension)
		'(javax.swing JPanel JFrame)
        )
		
;(:import [com.nativelibs4java.opencl CLContext CLByteBuffer CLMem CLMem$Usage CLEvent]
;	     [com.nativelibs4java.util NIOUtils]
;	     [java.nio ByteOrder ByteBuffer])
		
		
(use 'calx)
(use 'clojure.contrib.math)

(set! *warn-on-reflection* true)


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
;;;;; Iteration loops24
;Random number generator    , towards on-the-fly, self materalising liquids
;First UI just to show the random numbers....
  
(def sourceOpenCL
  "

uint randomnumber_fun(uint m_z_in, uint m_w_in)
{
uint m_z;
uint m_w;
m_z = 36969 * (m_z_in & 65535) + (m_z_in >> 16);
m_w = 18000 * (m_w_in & 65535) + (m_w_in >> 16);
return (m_z << 16) + m_w;
}
  
__kernel void randomnumbergen(
    __global uint *output_i)
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    output_i[gid] = randomnumber_fun(gid, gid ) % 1000;

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
(def gloal_size_clj (* 64 64))
(def innerloop_clj 10)

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
        InnerLoopCount innerloop_clj]
        
        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
        (def Buffer_int1   (create-buffer gloal_size :int32  ))
        (enqueue-barrier) (finish)		
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
        (loop [k InnerLoopCount]
                (do
                        (enqueue-kernel :randomnumbergen gloal_size Buffer_int1) 
                        (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
                        (finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))
        
        (def startnanotime_bufferReadOutTime (. System (nanoTime)))
        
        (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 4096])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 6400])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
        (def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
        (release! Buffer_int1)
        (enqueue-barrier)
        (finish)
    nil)))

;(println (vec @OpenCLoutputAtom1))
;(println (vec @OpenCLoutputAtom2))
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

(defn render [g]
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
      (for [x (range 1000) ]
	    (doto bg
		  (.setColor (. Color red))
	      (.drawLine  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
		              x 
					  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
					  x))))
	
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))))

	
(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim)
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))





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




