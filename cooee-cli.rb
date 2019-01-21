class CooeeCli < Formula
  desc "Coo.ee CLI"
  homepage "https://github.com/yschimke/cooee-cli"
  version "0.1"
  # url "file:///Users/yuri/workspace/cooee-cli/build/distributions/cooee-cli-master-ed2a449.tar"
#   sha256 "97c304c18a89fcbe59e2d0f8d6767f777ec811529713ca8796c93c41b6cb566d"
#   head "https://github.com/yschimke/cooee-cli.git"

  url "https://github.com/yschimke/cooee-cli.git"

  depends_on :java

  def install
    system "./gradlew", "installDist"

    libexec.install Dir["build/install/cooee-cli/*"]
    bin.install_symlink "#{libexec}/bin/cooee-cli" => "cooee"
    bash_completion.install "#{libexec}/bash/completion.bash" => "cooee"
    fish_completion.install "#{libexec}/fish/cooee.fish"
  end
end
