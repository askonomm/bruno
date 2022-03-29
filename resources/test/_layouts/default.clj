(declare post)
(declare load-partial)

[:div
 (load-partial "header" {'test "this"})
 [:div.post
  [:h2 (:title post)]
  [:div.entry (:entry post)]]]