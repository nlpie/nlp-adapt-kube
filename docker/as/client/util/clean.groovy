import java.util.regex.*
import java.text.Normalizer
import groovy.io.FileType

def currentDir = new File(this.args[0]); 

patterns = [
  [ pat:Pattern.compile(/(\d+\/\d+)-/), mat: '$1'],
  [ pat:Pattern.compile(/(\s+|^|\\n)\.(\D+)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\cM/), mat: ""],
  [ pat:Pattern.compile(/\p{Cntrl}&&[^\cJ\cM\cI]/), mat: ""],
  [ pat:Pattern.compile(/\P{ASCII}/), mat: ""],
  [ pat:Pattern.compile(/(\s+)\.+(\s*)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: ""],
  [ pat:Pattern.compile(/\|/), mat: " "]
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
