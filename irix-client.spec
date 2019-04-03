Name:           irix-client
Version:        2.1.0
Release:        1%{?dist}
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

%if 0%{?rhel}
# For EPEL, override the '_sharedstatedir' macro on RHEL
%define           _sharedstatedir    /var/lib
%endif

%description
A Client for generating IRIX docs from requests

%prep
%setup -q
tar xzf %{_topdir}/SOURCES/irix-dokpool-schema-2.0.0.tar.gz -C ./src/main/webapp/WEB-INF/irix-schema/ --strip-components=1

%build
mvn -DskipTests clean package

%install
rm -rf %{buildroot}

cd target
echo %{buildroot}%{_sharedstatedir}/tomcat/webapps
mkdir -p %{buildroot}%{_sharedstatedir}/tomcat/webapps
cp irix-client.war %{buildroot}%{_sharedstatedir}/tomcat/webapps

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root)
%{_sharedstatedir}/tomcat/webapps/*


%changelog
* Wed Apr 03 2019 Marco Lechner, Bundesamt fuer Strahlenschutz, Freiburg, Germany <mlechner@bfs.de> 2.1.0-1
- new spec file for RPM package build
