declare -A clients=(
[phos]="1.1.25"
[azul]="1.1.25"
[cmcom]="1.1.25"
[curb]="1.1.25"
[bgpostbank]="1.1.25"
[maxaa]="1.1.25"
[everypay]="1.1.25"
[jcc]="1.1.27"
)

for client in "${!clients[@]}"
do
  vers=${clients[$client]}
	branch="release/$client/$vers"

  echo "Building $client $vers from $branch"

  git checkout $branch

  buildPropsPlay="phos-sdk/build-config/$client-playstore.properties"
  buildOutputPlay="app/build/outputs/bundle/${client}ProdRelease/app-${client}-prod-release.aab"
  buildMappingPlay="app/build/outputs/mapping/${client}ProdRelease/mapping.txt"
  destPlay="../_buildOutput/${client}-playstore-v${vers}.aab"
  ./gradlew clean assembleWithDexProtector -Ppclient=$client -Ppenv=prod -Ppbuild=release -Ppinstall=false -Ppbundle=true -PpbuildProps=$buildPropsPlay
  cp $buildOutputPlay $destPlay
  cp $buildMappingPlay "$destPlay.mapping.txt"

  buildProps="phos-sdk/build-config/$client-non-playstore.properties"
  buildOutput="app/build/outputs/apk/${client}Prod/release/app-${client}-prod-release.apk"
  dest="../_buildOutput/${client}-non-playstore-v${vers}.apk"
  ./gradlew clean assembleWithDexProtector -Ppclient=$client -Ppenv=prod -Ppbuild=release -Ppinstall=false -Ppbundle=false -PpbuildProps=$buildProps
  cp $buildOutput $dest

done