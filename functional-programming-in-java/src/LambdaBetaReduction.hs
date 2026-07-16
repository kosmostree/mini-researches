main :: IO ()
main = print ((\f -> \g -> \x -> f (g x)) (+1) (*2) 5)   -- 11