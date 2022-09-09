# (too) many ways of defining class methods

# -----------------------------------
class A
  def self.cm
    "class method A" 
  end

  def im
    "instance method A"
  end

end

puts A.cm
puts A.im   # should not be found


# -----------------------------------
class B
end

def B.cm
  "B" 
end

put