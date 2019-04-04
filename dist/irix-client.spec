Name:           irix-client
Version:        2.1.0
Release:        2%{?dist}
Summary:        IRIX Client
URL:            http://www.imis.bfs.de
License:        GPLv3+
Group:          System Environment/Libraries
Source0:        https://github.com/OpenBfS/%{name}/archive/%{version}/%{name}-%{version}.tar.gz
Source1:        https://github.com/OpenBfS/irix-dokpool-schema/archive/2.0.0/irix-dokpool-schema-2.0.0.tar.gz
BuildArch:      noarch

BuildRequires:  apache-maven
Requires:       java
Requires:       tomcat >= 7.0.40

%global confdir %{_sysconfdir}/tomcat

%if 0%{?rhel}
# For EPEL, override the '_sharedstatedir' macro on RHEL
%define           _sharedstatedir    /var/lib
%endif

%description
A Client for generating IRIX docs from HTTO POST requests

%prep
%setup -q
tar xzf %{_sourcedir}/irix-dokpool-schema-2.0.0.tar.gz -C ./src/main/webapp/WEB-INF/irix-schema/ --strip-components=1

%build
mvn -DskipTests clean package

%install
rm -rf %{buildroot}

pushd target
mkdir -p %{buildroot}%{_sharedstatedir}/tomcat/webapps
cp irix-client.war %{buildroot}%{_sharedstatedir}/tomcat/webapps
popd

mkdir -p %{buildroot}%{confdir}/Catalina/localhost
pushd %{buildroot}%{confdir}/Catalina/localhost
echo '<?xml version="1.0" encoding="UTF-8"?>' > %{name}.xml
echo '<Context reloadable="true" privileged="true" altDDName="/etc/tomcat/conf.d/irix-client_web.xml">' >> %{name}.xml
echo '    <!-- **** Note - we have added in the reloadable and privileged attributes' >> %{name}.xml
echo '    to enable the invoker servlet and cgi support (other changes needed in' >> %{name}.xml
echo '    web.xml too for that, though **** -->' >> %{name}.xml
echo '    <WatchedResource>/etc/tomcat/conf.d/irix-client_web.xml</WatchedResource>' >> %{name}.xml
echo '</Context>' >> %{name}.xml
popd

mkdir -p %{buildroot}%{confdir}/conf.d
cp ./src/main/webapp/WEB-INF/web.xml %{buildroot}%{confdir}/conf.d/%{name}_web.xml

%clean
rm -rf %{buildroot}

%postun 
rm -rf %{_sharedstatedir}/tomcat/webapps/%{name}

%files
%defattr(-,root,root)
%{_sharedstatedir}/tomcat/webapps/*
%config(noreplace) %{confdir}/conf.d/%{name}_web.xml
%config(noreplace) %{confdir}/Catalina/localhost/%{name}.xml

%changelog
* Thu Apr 04 2019 Marco Lechner, Bundesamt fuer Strahlenschutz, Freiburg, Germany <mlechner@bfs.de> 2.1.0-2
- spec file modified - now having external web.xml
* Wed Apr 03 2019 Marco Lechner, Bundesamt fuer Strahlenschutz, Freiburg, Germany <mlechner@bfs.de> 2.1.0-1
- new spec file for RPM package build
