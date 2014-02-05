gconftool-2 -t string -s /desktop/gnome/url-handlers/worldcoin/command "java -splash:doesnotexist.png -jar $INSTALL_PATH/worldcoin-wallet-exe.jar %s"
gconftool-2 -s /desktop/gnome/url-handlers/worldcoin/needs_terminal false -t bool
gconftool-2 -t bool -s /desktop/gnome/url-handlers/worldcoin/enabled true