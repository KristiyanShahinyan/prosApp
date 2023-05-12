This guide should present all details needed to setup the Phos app project and run dev/prod builds.

1. Requirements
1.1 Gradle plugin 3.4.2
Set in build.gradle in the root project folder. Look for dependencies, com.android.tools.build:gradle.

1.2 Android SDK 28
Set in build.gradle in the app folder. Look for the compileSdkVersion.

1.3 Android NDK r19
- Download link: https://developer.android.com/ndk/downloads/older_releases.html#ndk-19c-downloads
- Set in local.properties in the root project folder

1.4 DexProtector 9.7.19
- Download link (ask for access permission): https://outlook.office.com/mail/group/paynetics.digital/development/files/sites/development/Shared%20Documents/Phos/dexprotector
- Set the path to the root DexProtector folder (the flatDir property) in build.gradle(Project: android_phos)
- To activate DexProtector copy dexprotector.licel to C:\Users\your user account
- For more details check https://dexprotector.com/docs#installation

1.5 Fetch all required git submodules
# to pull modules initially
git submodule update --init --recursive
# to update (or via git pull master for each module)
git submodule update --recursive
Notes:
- Check .gitmodules for all required modules
- Tutorial on git modules https://gist.github.com/gitaarik/8735255

1.6 Release builds
Enter password for the signing cert in keystore.properties in the root project folder.
Ask Paynetics team for the password.

2. How to switch DexProtector off
DexProtector is on by default for all debug and release build variants. You can turn it off by
commenting "apply plugin: 'dexprotector'" in build.gradle(Module: app). Do this only for
dev/debug purposes. As an alternative you can use custom Gradle tasks listed below.

3. How to build
For convenience you can use these custom gradle tasks:
- startWithoutDexProtector
- assembleWithoutDexProtector
- assembleWithChecksumCalc
You can pass the following parameters:
- pclient: phos, computop
- penv: prod, stage, app, cloud, internal or localhost
- pbuild: debug or release
- pinstall: true to install the apk automatically after build

3.0 Prerequisites
3.0.1 Select branch feat_ids in module kernel_mastercard
3.0.2 Select branch androidx-migration in module reader
3.0.3 Select branch master in all other modules
3.0.4 Do a git tag (e.g. 'git tag 1.0.1' or 'git tag 1.00.01')

3.1 Dev apk without DexProtector
gradlew startWithoutDexProtector -Ppclient=phos -Ppenv=app -Ppbuild=debug -Ppinstall=true

3.2 Prod (official play store apk)
gradlew assembleWithDexProtector -Ppclient=phos -Ppenv=prod -Ppbuild=release -Ppinstall=false

4. Constants
public enum Type {
    SELECT_NEXT, /* Kernel internal - more than one application available, and the current does not satisfy the requirements */
    TRY_AGAIN, /* Read timeout - card did not read correctly. Please retry with the same card and be more patient */
    APPROVED, /* In tests only - transaction approved offline. Should not happen in production */
    DECLINED, /* Declined by the kernel because of decline by the card */
    ONLINE_REQUEST, /* Kernel decided that we need to send this transaction to the host. Should happen on success */
    TRY_ANOTHER_INTERFACE, /* Card and kernel do not want to communicate using this interface. We don't have one, so decline */
    END_APPLICATION /* Transaction did not process successfully, because of internal error in kernel */
}

public enum Cvm {
    ONLINE_PIN, /* Kernel decided that transaction can only happen if online pin is provided */
    CONFIRMATION_CODE_VERIFIED, /* OD CVM provided and correct, send to host */
    OBTAIN_SIGNATURE, /* If signature is supported, ask the customer to sign the receipt. Also print "signature req." on it */
    NO_CVM, /* No CVM required, send to host */
    NOT_APPLICABLE /* CVM does not mean anything in this context e.g. declined */
}
