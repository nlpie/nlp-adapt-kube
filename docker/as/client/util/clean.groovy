import java.util.regex.*
import java.text.Normalizer
import groovy.io.FileType

def currentDir = new File(this.args[0]); 

patterns = [
  [ pat:Pattern.compile(/(\d+\/\d+)(-)/), mat: {global, x, y -> "$x" + " ".multiply(y.size())}],
  [ pat:Pattern.compile(/(\s+|^|\\n)(\.)(\D+)/), mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:Pattern.compile(/^\cM/), mat: ""],
  [ pat:Pattern.compile(/\p{Cntrl}&&[^\cJ\cM\cI]/), mat: ""],
  [ pat:Pattern.compile(/\P{ASCII}/), mat: {x -> " ".multiply(x.size())}],
  [ pat:Pattern.compile(/(\s+)(\.+)(\s*)/), mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: {global -> " ".multiply(global.size())}],
  [ pat:Pattern.compile(/\|/), mat: {global -> " ".multiply(global.size())}]
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
