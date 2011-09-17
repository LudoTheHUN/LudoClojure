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
        '(java.awt Color Graphics Graphics2D)
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
;;;;; Iteration loops21
;


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
;(def OpenCLoutputAtom1 (atom [1]))
;(def OpenCLoutputAtom2 (atom [1]))
;(def OpenCLoutputAtom3 (atom [1]))
;(def OpenCLoutputAtom4 (atom [1]))
;(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size (* 64 64 64))
(def globalsize global_clj_size)
(def localsize 16)
(def InnerLoopCount 16)
;(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)





								   


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)]

;;(def CustomLargeByteBuffer  (create-byte-buffer [(* 64 64)]))
;;(println "type CustomLargeByteBuffer: "(type CustomLargeByteBuffer))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
	(let [cl_localsize localsize
	     ; CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 )    ;create an empty buffer
		  ;clbuffer_b (create-buffer global_clj_size :float32 )
          ]
		  
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN  --OR direct (wrap...) into kernel
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the atom's contents as data is now on the GPU
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
        (def clbuffer_b (mimic clbuffer_a))
		(def clbuffer_c (mimic clbuffer_a))
		
		;(println "type clbuffer_a: "(type clbuffer_a))
		;(println "type CustomLargeBuffer: "(type CustomLargeBuffer))
		;(def CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))     ;Createing arbitrary large buffer
		
		;;Would be nice to be ableto to write to it...
		
	;	(enqueue-overwrite clbuffer_c [0 3] 
	;	  (.createByteBuffer (context) CLMem$Usage/InputOutput (to-buffer [0.13 0.23 0.34 0.45] :float32) false))  ;(usage-types :in-out)-> CLMem$Usage/InputOutput
		;OK  (def  CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))
		;BROKEN(CustomLargeBuffer (to-buffer (wrap @OpenCLoutputAtom2 :float32) :float32))

		;;There seem to be two types of buffer... calx.data.Buffer and java.nio.ByteBuffer.
		
		(enqueue-barrier) (finish)
	    (def startnanotime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do 	;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)  
						(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize) (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c              cl_localsize) (enqueue-barrier)
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;NOTE; already is nuked by this point since the load.Nuke the old data so that the write it will not take double space due to the transactional swap!
	;	(swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
		
		;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read CustomLargeBuffer [0 24])))))   ;OPTIMAL RADOUT OPTION: Could loop to read out parts of the buffer at a time OPTIMAL 
		(enqueue-barrier) (finish)
		;(release! clbuffer_a)
		(release! clbuffer_b)
		;(release! clbuffer_c)
		;(release! CustomLargeBuffer)   ;Note, without enqueue-read, we are leaking GPU RAM.    JAVA GC (garbace collector) needs to kick in to actualy release the reserved data from the GPU, distroying old versions that got (def...) earlier. This takes time(can not be forced?) and thuse it will be possible to run out of RAM.  Recomendation is to stay within the inner kernel loop, swapping data into buffers, reading out when needed... but never 'top level' looping, thus effectively working with constant GPU RAM utalisation via buffer overwrites. Just need to figure direct 'write in' into an existing buffer.
		(enqueue-barrier)
		(finish)
	nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 0))

(println (vec @OpenCLoutputAtom3))
									
									
(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)

		 
))



(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)


(defmacro swapIn_atom! [atomname global_clj_size atomgeneraorfunction]
  `(let [global_clj_size# ~global_clj_size]
	(def ~atomname (atom []))   ; A pre data nuke to reduce RAM footptint during swapin
    (swap! ~atomname (fn [~atomname] (~atomgeneraorfunction global_clj_size#))) ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
    (println "(type @" '~atomname ") :" (type (deref ~atomname)))
    (println "(nth @" '~atomname " 0):" (nth (deref ~atomname) 0))))

(macroexpand-1 '(swapIn_atom! OpenCLoutputAtom2 (* global_clj_size 1) randomarraygenerator))

(defmacro makeatom! [atomame]
  `(do (def ~atomame (atom [1]))
   (swap! ~atomame (fn [~atomame] 1))))

 (do (def boo (atom [1]))
   (swap! boo (fn [_] 1)))
   
(makeatom! fooooo)
(macroexpand-1 '(makeatom! fooooo))
@fooooo
(class fooooo)

(string? 'OpenCLoutputAtom2)
(resolve 'foo)


;;Note, as we develop better liquids, we can just define them with these liquid generating functions
(defn randomarraygenerator [global_clj_size]
  (^floats float-array global_clj_size
    (for [i (range global_clj_size)] (float (rand)))))
	
(def global_clj_size (* 64 64 64 4))
;(swapIn_OpenCLoutputAtom2! (* global_cljze_si 1))
(swapIn_atom! OpenCLoutputAtom2 (* global_clj_size 1) randomarraygenerator)
(swapIn_atom! OpenCLoutputAtom3 16 randomarraygenerator)
(testoutputs global_clj_size 1  10000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)


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




