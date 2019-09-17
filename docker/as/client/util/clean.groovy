import java.util.regex.*
import java.text.Normalizer
import groovy.io.FileType

def currentDir = new File(this.args[0]); 

patterns = [
  [ pat:~/(\d+\/\d+)(-)/, mat: {global, x, y -> "$x" + " ".multiply(y.size())}],
  [ pat:~/(\s+|^|\\n)(\.)(\D+)/, mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:~/^\cM/, mat: ""],
  [ pat:~/\p{Cntrl}&&[^\cJ\cM\cI]/, mat: ""],
  [ pat:~/\P{ASCII}/, mat: {x -> " ".multiply(x.size())}],
  [ pat:~/(\s+)(\.+)((?!\d)\s*)/, mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: {global -> " ".multiply(global.size())}],
  [ pat:~/\|/, mat: {global -> " ".multiply(global.size())}]
];

def filerep(file){
  print "Normalizing $file.name ...";
  def norm = Normalizer.normalize(file.text, Normalizer.Form.NFD);
  for ( repl in patterns ) {
    while(norm =~ repl.pat){
      norm = norm.replaceAll(repl.pat, repl.mat);
    }
  }

  if(file.text == norm){
    println ""
  } else {
    println "Changed."
  }
  
  file.write(norm);
}


currentDir.eachFileRecurse(FileType.FILES){ file -> filerep(file) }
