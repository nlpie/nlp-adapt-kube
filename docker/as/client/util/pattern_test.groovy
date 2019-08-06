import java.util.regex.*
import java.text.Normalizer

def patterns = [
  [ pat:Pattern.compile(/(\d+\/\d+)-/), mat: '$1'],
  // [ pat:Pattern.compile(/\.(\S\/\S)/), mat: '$1'],
  [ pat:Pattern.compile(/(\s+|^|\\n)\.(\D+)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\cM/), mat: ""],
  [ pat:Pattern.compile(/\p{Cntrl}&&[^\cJ\cM\cI]/), mat: ""],
  [ pat:Pattern.compile(/\P{ASCII}/), mat: ""],
  // [ pat:Pattern.compile(/(\\n)\./), mat: '$1'],
  [ pat:Pattern.compile(/(\s+)\.+(\s*)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: ""],
  [ pat:Pattern.compile(/\|/), mat: " "]
]

def norm = Normalizer.normalize(this.args[0], Normalizer.Form.NFD);
for ( repl in patterns ) {
  while(norm =~ repl.pat){
    norm = norm.replaceAll(repl.pat, repl.mat);
  }
}

println norm;
