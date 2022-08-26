
require 'ripper'
require 'pp'
require 'json'
require 'optparse'


# --------------------- utils ---------------------
def banner(s)
  puts "\033[93m#{s}:\033[0m"
end


class AstSimplifier

  def initialize(filename)
    @filename = filename

    f = File.open(filename, 'rb')
    @src = f.read
    f.close

    detected_enc = detect_encoding(@src)
    if detected_enc
      begin
        @src.force_encoding(detected_enc)
      rescue
        @src.force_encoding('utf-8')
      end