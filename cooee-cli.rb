class CooeeCli < Formula
  desc "Coo.ee CLI"
  homepage "https://github.com/yschimke/cooee-cli"
  version "0.1"
  url "file:///Users/yuri/workspace/cooee-cli/build/distributions/cooee-cli-master-79cc045.tar"
#   sha256 "97c304c18a89fcbe59e2d0f8d6767f777ec811529713ca8796c93c41b6cb566d"

  depends_on :java

  def install
    libexec.install Dir["*"]
    bin.install_symlink "#{libexec}/bin/cooee-cli" => "cooee"
    bash_completion.install "#{libexec}/bash/completion.bash" => "cooee"
  end
end
