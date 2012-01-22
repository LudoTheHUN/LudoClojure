(import ;imports necessary just like in Java                                
'(java.awt Graphics)
'(javax.swing.event ChangeListener ChangeEvent)
'(javax.swing JPanel JFrame JButton)
)
(import 'java.awt.event.ActionListener)

;; Based on:
;; http://lifeofaprogrammergeek.blogspot.com/2009/03/learning-clojure-and-emacs.html

(defn render [#^Graphics g] ;"#^" is an optional type hint to the compiler  
(doto g ;"doto" lets you call successive functions on an object        
(.drawString "Hello World!" 10 20)));draw "Hello World!" on the graphics passed in
(def panel (proxy [JPanel] [] ;proxy creates anonymous subclasses           
(paint [g] (render g)))); this line overrides JPanel's paint method
(def frame (doto (new JFrame);doto returns its first argument               
(.add panel);add the panel to the frame                        
(.setBounds 100 100 100 60);set the dimensions of the frame    
(.setVisible true)));show the frame


(defn render [#^Graphics g] ;"#^" is an optional type hint to the compiler  
(doto g ;"doto" lets you call successive functions on an object        
(.drawString "Hello Worerereld!" 10 20)))




(defn printclicked []
(println "Clicked!"))


(doto (JFrame.)
(.add
(doto (JButton. "Click me!")
(.addActionListener
 (proxy [ActionListener] []
   (actionPerformed [e] (printclicked))))))
(.setBounds 400 400 200 200)
(.setVisible true))


(defn printclicked []
(println "Clicdfdked!"))





