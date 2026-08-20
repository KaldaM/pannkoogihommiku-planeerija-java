Summary: APPLICATION_SUMMARY
Name: APPLICATION_PACKAGE
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: APPLICATION_LICENSE_TYPE
Vendor: APPLICATION_VENDOR

%if "xAPPLICATION_URL" != "x"
URL: APPLICATION_URL
%endif

%if "xAPPLICATION_PREFIX" != "x"
Prefix: APPLICATION_PREFIX
%endif

Provides: APPLICATION_PACKAGE
Provides: pannkoogihommiku-planeerija
Obsoletes: pannkoogihommiku-planeerija

%if "xAPPLICATION_GROUP" != "x"
Group: APPLICATION_GROUP
%endif

Autoprov: 0
Autoreq: 0
%if "xPACKAGE_DEFAULT_DEPENDENCIES" != "x" || "xPACKAGE_CUSTOM_DEPENDENCIES" != "x"
Requires: PACKAGE_DEFAULT_DEPENDENCIES PACKAGE_CUSTOM_DEPENDENCIES
%endif

%define __jar_repack %{nil}
%define _build_id_links none

%define package_filelist %{_builddir}/%{name}.files
%define app_filelist %{_builddir}/%{name}.app.files
%define filesystem_filelist %{_builddir}/%{name}.filesystem.files
%define default_filesystem / /opt /usr /usr/bin /usr/lib /usr/local /usr/local/bin /usr/local/lib

%description
APPLICATION_DESCRIPTION

%global __os_install_post %{nil}

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}APPLICATION_DIRECTORY
cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY
if [ "$(echo %{_sourcedir}/lib/systemd/system/*.service)" != '%{_sourcedir}/lib/systemd/system/*.service' ]; then
  install -d -m 755 %{buildroot}/lib/systemd/system
  cp %{_sourcedir}/lib/systemd/system/*.service %{buildroot}/lib/systemd/system
fi
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:APPLICATION_LICENSE_FILE}
  install -d -m 755 "%{buildroot}%{dirname:%{license_install_file}}"
  install -m 644 "APPLICATION_LICENSE_FILE" "%{buildroot}%{license_install_file}"
%endif

for size in 16 22 32 48 64 128 256; do
  case "$size" in
    128) source_icon="%{_sourcedir}APPLICATION_DIRECTORY/lib/icons/plaanisepp-plan-128.png" ;;
    256) source_icon="%{_sourcedir}APPLICATION_DIRECTORY/lib/icons/plaanisepp-app-256.png" ;;
    *) source_icon="%{_sourcedir}APPLICATION_DIRECTORY/lib/icons/plaanisepp-${size}.png" ;;
  esac
  install -d -m 755 "%{buildroot}/usr/share/icons/hicolor/${size}x${size}/apps"
  install -d -m 755 "%{buildroot}/usr/share/icons/hicolor/${size}x${size}/mimetypes"
  install -m 644 "$source_icon" "%{buildroot}/usr/share/icons/hicolor/${size}x${size}/apps/plaanisepp.png"
  install -m 644 "$source_icon" "%{buildroot}/usr/share/icons/hicolor/${size}x${size}/mimetypes/application-x-pannkoogihommiku-plan.png"
  install -m 644 "$source_icon" "%{buildroot}/usr/share/icons/hicolor/${size}x${size}/mimetypes/pannkoogihommiku-plan.png"
done
for mime_info in %{buildroot}APPLICATION_DIRECTORY/lib/*-MimeInfo.xml; do
  sed -i '/<mime-type type="application\/x-pannkoogihommiku-plan">/a\    <icon name="pannkoogihommiku-plan"></icon>' "$mime_info"
done
rm -rf %{buildroot}APPLICATION_DIRECTORY/lib/icons

(cd %{buildroot} && find . -path ./lib/systemd -prune -o -type d -print) | sed -e 's/^\.//' -e '/^$/d' | sort > %{app_filelist}
{ rpm -ql filesystem || echo %{default_filesystem}; } | sort > %{filesystem_filelist}
comm -23 %{app_filelist} %{filesystem_filelist} > %{package_filelist}
sed -i -e 's/.*/%dir "&"/' %{package_filelist}
(cd %{buildroot} && find . -not -type d) | sed -e 's/^\.//' -e 's/.*/"&"/' >> %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  sed -i -e 's|"%{license_install_file}"||' -e '/^$/d' %{package_filelist}
%endif

%files -f %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %license "%{license_install_file}"
%endif

%post
package_type=rpm
LAUNCHER_AS_SERVICE_SCRIPTS
LAUNCHER_AS_SERVICE_COMMANDS_INSTALL

%posttrans
package_type=rpm
DESKTOP_COMMANDS_INSTALL
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
  gtk-update-icon-cache -f -t /usr/share/icons/hicolor >/dev/null 2>&1 || :
fi
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database /usr/local/share/applications >/dev/null 2>&1 || :
fi
if command -v update-mime-database >/dev/null 2>&1; then
  update-mime-database /usr/share/mime >/dev/null 2>&1 || :
fi

%pre
package_type=rpm
COMMON_SCRIPTS
LAUNCHER_AS_SERVICE_SCRIPTS
if [ "$1" -gt 1 ]; then
  :; LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL
fi

%preun
package_type=rpm
COMMON_SCRIPTS
DESKTOP_SCRIPTS
LAUNCHER_AS_SERVICE_SCRIPTS
if [ "$1" -eq 0 ]; then
  DESKTOP_COMMANDS_UNINSTALL
  LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL
fi

%postun
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
  gtk-update-icon-cache -f -t /usr/share/icons/hicolor >/dev/null 2>&1 || :
fi
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database /usr/local/share/applications >/dev/null 2>&1 || :
fi
if command -v update-mime-database >/dev/null 2>&1; then
  update-mime-database /usr/share/mime >/dev/null 2>&1 || :
fi

%clean
