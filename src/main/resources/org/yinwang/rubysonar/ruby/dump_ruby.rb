
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
    else
      # default to UTF-8
      @src.force_encoding('utf-8')
    end

    @src.encode('utf-8',
                {:undef => :replace,
                 :invalid => :replace,
                 :universal_newline => true}
    )

    @line_starts = [0]
    find_line_starts
    find_docs
  end


  def detect_encoding(s)
    # take first two lines
    header = s.match('^.*\n?.*\n?')
    if header and header[0]
      matched = header[0].match('^\s*#.*coding\s*[:=]\s*([\w\d\-]+)')
      if matched and matched[1]
        matched[1]
      end
    end
  end


  # initialize the @line_starts array
  # used to convert (line,col) location to (start,end)
  def find_line_starts
    lines = @src.split(/\n/)
    total = 0
    lines.each { |line|
      total += line.length + 1 # line and \n
      @line_starts.push(total)
    }
  end


  def find_docs
    @docs = {}
    lines = @src.split(/\n/)
    first_line = nil
    current_line = 0
    accum = []

    lines.each { |line|
      matched = line.match('^\s*#\s*(.*)')
      if matched
        accum.push(matched[1])
        if !first_line
          first_line = current_line
        end
      elsif !accum.empty?
        doc = {
            :type => :string,
            :id => accum.join('\n'),
        }
        @docs[current_line+1] = doc
        @docs[first_line-1] = doc
        accum.clear
        first_line = nil
      end

      current_line += 1
    }
  end


  def node_start(loc)
    line = loc[0]
    col = loc[1]
    @line_starts[line-1] + col
  end


  def ident_end(start_idx)
    if @src[start_idx] == '[' and @src[start_idx + 1] == ']'
      return start_idx + 2
    end
    idx = start_idx
    while idx < @src.length and @src[idx].match /[[:alpha:]0-9_@$\?!]/
      idx += 1
    end
    idx
  end


  def simplify
    tree = Ripper::SexpBuilder.new(@src).parse

    if $options[:debug]
      banner 'sexp'
      pp tree
    end

    simplified = convert(tree)
    simplified = find_locations(simplified)

    if $options[:debug]
      banner 'simplified'
      pp simplified
    end

    simplified
  end


  def find_locations(obj)
    def find1(obj)